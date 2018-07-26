package cn.academy.vanilla.vecmanip.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context._
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import jdk.nashorn.internal.ir.BlockStatement
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.math.{BlockPos, Vec3d}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

object DirectedBlastwave extends Skill("dir_blast", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, player => new BlastwaveContext(player))
  }

}

private object BlastwaveContext {
  final val MSG_EFFECT  = "effect"
  final val MSG_PERFORM = "perform"
  final val MSG_ATTACK_ENTITY = "entity"
  final val MSG_GENERATE_EFFECT_BLOCKS = "effect_blocks"
}

import cn.academy.ability.api.AbilityAPIExt._
import BlastwaveContext._
import MCExtender._
import cn.academy.vanilla.vecmanip.client.effect.AnimPresets._
import cn.academy.ability.api.AbilityPipeline._
import MathUtils._
import cn.lambdalib2.util.generic.RandUtils._
import scala.collection.JavaConversions._

class BlastwaveContext(p: EntityPlayer) extends Context(p, DirectedBlastwave) with IConsumptionProvider {

  val MIN_TICKS = 6
  val MAX_ACCEPTED_TICKS = 50
  val MAX_TOLERANT_TICKS = 200
  val PUNCH_ANIM_TICKS = 6

  var ticker = 0

  var punched = false
  var punchTicker = 0

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    if (ticker > MIN_TICKS && ticker < MAX_ACCEPTED_TICKS) {
      sendToServer(MSG_PERFORM, ticker.asInstanceOf[AnyRef])
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = terminate()

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tick() = if (isLocal) {
    ticker += 1
    if (ticker >= MAX_TOLERANT_TICKS) {
      terminate()
    }
    if (punched) {
      punchTicker += 1
    }
    if (punched && punchTicker > PUNCH_ANIM_TICKS) {
      terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform(ticks: Int) = {
    if (tryConsume()) {
      val trace: TraceResult = Raytrace.traceLiving(player, 4, EntitySelectors.living)
      val position = trace match {
        case EmptyResult() => player.position + player.lookVector * 4
        case EntityResult(ent) => ent.headPosition
        case res if res.hasPosition => res.position
      }

      ctx.setCooldown(cooldown)
      sendToClient(MSG_PERFORM, position)

      var effective = false

      // Hurt entities around
      val entities = WorldUtils.getEntities(world,
        position.xCoord, position.yCoord, position.zCoord,
        3, EntitySelectors.exclude(player)).toList

      entities.foreach (entity => {
        ctx.attack(entity, damage)
        knockback(entity)

        val delta = (entity.position - player.position).normalize() * 0.24
        entity.setVel(entity.velocity + delta)

        effective = true
      })

      sendToClient(MSG_ATTACK_ENTITY, entities.toArray)

      // Destroy blocks around
      {
        def ran(x: Int) = (x - 3) until (x + 3)
        val (x, y, z) = (math.round(position.x).floor.toInt, math.round(position.y).floor.toInt, math.round(position.z).floor.toInt)

        for {i <- ran(x)
             j <- ran(y)
             k <- ran(z)} {
          val (dx, dy, dz) = (i - x, j - y, k - z)
          val distSq = dx * dx + dy * dy + dz * dz
          if ((distSq <= 6) && (distSq == 0 || RNG.nextFloat() < breakProb)) {
            val bPos = new BlockPos(i, j, k)
            val state = world.getBlockState(bPos)
            val block = state.getBlock
            val meta = block.getMetaFromState(state)
            val hardness = block.getBlockHardness(state, world, null)
            if (0 <= hardness && hardness <= breakHardness && ctx.getSkillExp==1f)
            {
              val itemStack=new ItemStack(block)
              world.spawnEntity(new EntityItem(world,i,j,k,itemStack))
              world.setBlockToAir(bPos)
            }
            else if (0 <= hardness && hardness <= breakHardness && ctx.canBreakBlock(world, i, j, k)) {
              // This line causes the sound effect unable to be heard.
              // So strange...
              //> world.playSoundEffect(i + 0.5, j + 0.5, k + 0.5, block.stepSound.getBreakSound, .5f, 1f)

              if (RNG.nextFloat() < dropRate) {
                block.dropBlockAsItemWithChance(world, i, j, k, world.getBlockMetadata(i, j, k), 1.0f, 0)
              }

              world.setBlockToAir(bPos)
            }
          }
        }
      }

      sendToClient(MSG_GENERATE_EFFECT_BLOCKS,new Vec3d()(position.xCoord, position.yCoord, position.zCoord))

      ctx.addSkillExp(if (effective) 0.0025f else 0.0012f)

      DirectedBlastwave.triggerAchievement(player)
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_ATTACK_ENTITY, side=Array(Side.CLIENT))
  def c_effect(entities: Array[Entity]) = {
    entities.foreach(knockback)
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform(pos: Vec3d) = {
    sendToSelf(MSG_EFFECT, new Vec3d(pos.x, pos.y, pos.z))

    punched = true
  }

  private def tryConsume() = {
    val overload = lerpf(50, 30, ctx.getSkillExp)

    ctx.consume(overload, consumption)
  }

  override def getConsumptionHint = consumption

  private val consumption = lerpf(160, 200, ctx.getSkillExp)

  private val breakProb = lerpf(0.5f, 0.8f, ctx.getSkillExp)

  private val breakHardness = ctx.getSkillExp match {
    case exp if exp < 0.25f => 2.9f
    case exp if exp < 0.5f => 25f
    case _ => 55f
  }

  private val damage = lerpf(10, 25, ctx.getSkillExp)

  private val dropRate = lerpf(0.4f, 0.9f, ctx.getSkillExp)

  private val cooldown = lerpf(80, 50, ctx.getSkillExp).toInt

  private def knockback(targ: Entity) = {
    var delta = player.headPosition - targ.headPosition
    delta = delta.normalize()
    delta.y = -0.4f
    delta = delta.normalize()

    targ.setPosition(targ.posX, targ.posY + 0.1, targ.posZ)
    targ.setVel(delta * -1.2f)
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[BlastwaveContext])
class BlastwaveContextC(par: BlastwaveContext) extends ClientContext(par) {

  var handEffect: HandRenderer = _

  var anim: CompTransformAnim = _

  var timeProvider: () => Double = null

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def effectAt(pos: Vec3) = {
    ACSounds.playClient(world, pos.x, pos.y, pos.z, "vecmanip.directed_blast", 0.5f, 1.0f)

    val effect = new WaveEffect(world, rangei(2, 3), 1)
    effect.setPos(util.mc.Vec3.lerp(player.headPosition, pos, 0.7))
    effect.rotationYaw = player.rotationYawHead + rangef(-20, 20)
    effect.rotationPitch = player.rotationPitch + rangef(-10, 10)

    world.spawnEntityInWorld(effect)
  }


  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_handEffectStart() = if (isLocal) {
    anim = createPrepareAnim()

    val init = GameTimer.getTime
    timeProvider = () => {
      val dt = GameTimer.getTime - init
      math.min(2.0, dt / 150.0)
    }

    handEffect = new HandRenderer {
      override def render(partialTicks: Float) = {
        anim.perform(timeProvider())
        HandRenderer.renderHand(partialTicks, anim.target)
      }
    }

    HandRenderInterrupter(player).addInterrupt(handEffect)
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_handEffectTerminate() = if (isLocal) {
    HandRenderInterrupter(player).stopInterrupt(handEffect)
  }

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def l_effect() = if (isLocal) {
    val init = GameTimer.getTime
    timeProvider = () => {
      val dt = GameTimer.getTime - init
      dt / 300.0
    }

    anim = createPunchAnim()
    anim.perform(0)
  }

}