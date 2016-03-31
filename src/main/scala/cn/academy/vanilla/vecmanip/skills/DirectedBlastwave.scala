package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{IConsumptionProvider, Context, ClientRuntime}
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util
import cn.lambdalib.util.mc._
import cn.lambdalib.vis.animation.presets.CompTransformAnim
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.Vec3

object DirectedBlastwave extends Skill("dir_blast", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, player => new BlastwaveContext(player))
  }

}

private object BlastwaveContext {
  final val MSG_PERFORM = "perform"
  final val MSG_ATTACK_ENTITY = "entity"
  final val MSG_GENERATE_EFFECT_BLOCKS = "effect_blocks"
}

class BlastwaveContext(p: EntityPlayer) extends Context(p) with IConsumptionProvider {

  import cn.academy.ability.api.AbilityAPIExt._
  import BlastwaveContext._
  import MCExtender._
  import cn.academy.vanilla.vecmanip.client.effect.AnimPresets._
  import cn.academy.ability.api.AbilityPipeline._
  import MathUtils._
  import cn.lambdalib.util.generic.RandUtils._
  import scala.collection.JavaConversions._

  implicit val skill_ = DirectedBlastwave
  implicit val aData_ = aData
  implicit val player_ = p

  val MIN_TICKS = 6
  val MAX_ACCEPTED_TICKS = 50
  val MAX_TOLERANT_TICKS = 200
  val PUNCH_ANIM_TICKS = 6

  var ticker = 0

  var punched = false
  var punchTicker = 0

  @SideOnly(Side.CLIENT)
  var handEffect: HandRenderer = null

  @SideOnly(Side.CLIENT)
  var anim: CompTransformAnim = null

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
    if (consume()) {
      val trace: TraceResult = Raytrace.traceLiving(player, 4, EntitySelectors.living)
      val position = trace match {
        case EmptyResult() => player.position + player.lookVector * 4
        case EntityResult(ent) => ent.headPosition
        case res if res.hasPosition => res.position
      }

      addSkillCooldown(cooldown)
      sendToClient(MSG_PERFORM, position)

      var effective = false

      // Hurt entities around
      val entities = WorldUtils.getEntities(world,
        position.xCoord, position.yCoord, position.zCoord,
        3, EntitySelectors.excludeOf(player)).toList

      entities.foreach (entity => {
        attack(player, DirectedShock, entity, damage)
        knockback(entity)

        val delta = (entity.position - player.position).normalize() * 0.24
        entity.setVel(entity.velocity + delta)

        effective = true
      })

      sendToClient(MSG_ATTACK_ENTITY, entities.toArray)

      // Destroy blocks around
      {
        def ran(x: Int) = (x - 3) until (x + 3)
        val (x, y, z) = (math.round(position.x).toInt, math.round(position.y).toInt, math.round(position.z).toInt)

        for {i <- ran(x)
             j <- ran(y)
             k <- ran(z)} {
          val (dx, dy, dz) = (i - x, j - y, k - z)
          val distSq = dx * dx + dy * dy + dz * dz
          if ((distSq <= 6) && (distSq == 0 || RNG.nextFloat() < breakProb)) {
            val block = world.getBlock(i, j, k)
            val meta = world.getBlockMetadata(i, j, k)
            val hardness = block.getBlockHardness(world, i, j, k)
            if (hardness <= breakHardness) {
              // This line causes the sound effect unable to be heard.
              // So strange...
              //> world.playSoundEffect(i + 0.5, j + 0.5, k + 0.5, block.stepSound.getBreakSound, .5f, 1f)

              if (RNG.nextFloat() < dropRate) {
                block.dropBlockAsItemWithChance(world, i, j, k, world.getBlockMetadata(i, j, k), 1.0f, 0)
              }

              world.setBlock(i, j, k, Blocks.air)

              // Minecraft.getMinecraft.effectRenderer.addBlockDestroyEffects(i, j, k, block, meta)
            }
          }
        }
      }

      sendToClient(MSG_GENERATE_EFFECT_BLOCKS, Vec3.createVectorHelper(position.xCoord, position.yCoord, position.zCoord))

      addSkillExp(if (effective) 0.0025f else 0.0012f)
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_handEffectStart() = if (isLocal) {
    anim = createPrepareAnim()

    handEffect = new HandRenderer {
      override def render(partialTicks: Float) = {
        HandRenderer.renderHand(partialTicks, anim.target)
      }
    }

    HandRenderInterrupter(player).addInterrupt(handEffect)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_handEffectTick() = if (isLocal) {
    if (!punched) {
      val time = MathUtils.clampd(0, 2.0, ticker.toDouble / 3.0)
      anim.perform(time)
    } else {
      val time = MathUtils.clampd(0, 1.0, punchTicker.toDouble / PUNCH_ANIM_TICKS)
      anim.perform(time)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_handEffectTerminate() = if (isLocal) {
    HandRenderInterrupter(player).stopInterrupt(handEffect)
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def l_effect() = if (isLocal) {
    punched = true

    anim = createPunchAnim()
    anim.perform(0)
  }

  @Listener(channel=MSG_ATTACK_ENTITY, side=Array(Side.CLIENT))
  def c_effect(entities: Array[Entity]) = {
    entities.foreach(knockback)
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform(pos: Vec3) = {
    ACSounds.playClient(player, "vecmanip.directed_shock", 0.5f)
    effectAt(util.mc.Vec3(pos.xCoord, pos.yCoord, pos.zCoord))
  }

  private def consume() = {
    val cp = consumption
    val overload = lerpf(96, 72, skillExp)

    cpData.perform(overload, cp)
  }

  override def getConsumptionHint = consumption

  private lazy val consumption = lerpf(200, 150, skillExp)

  private lazy val breakProb = lerpf(0.5f, 0.8f, skillExp)

  private lazy val breakHardness = skillExp match {
    case exp if exp < 0.25f => 2.9f
    case exp if exp < 0.5f => 25f
    case _ => 55f
  }

  private lazy val damage = lerpf(10, 18, skillExp)

  private lazy val dropRate = lerpf(0.4f, 0.9f, skillExp)

  private lazy val cooldown = lerpf(40, 20, skillExp).toInt

  private def knockback(targ: Entity) = {
    var delta = player.headPosition - targ.headPosition
    delta = delta.normalize()
    delta.yCoord = -0.4f
    delta = delta.normalize()

    targ.setPosition(targ.posX, targ.posY + 0.1, targ.posZ)
    targ.setVel(delta * -1.2f)
  }

  private def effectAt(pos: Vec3) = {
    val effect = new WaveEffect(world, rangei(2, 3), 1)
    effect.setPos(util.mc.Vec3.lerp(player.headPosition, pos, 0.7))
    effect.rotationYaw = player.rotationYawHead + rangef(-20, 20)
    effect.rotationPitch = player.rotationPitch + rangef(-10, 10)

    world.spawnEntityInWorld(effect)
  }
}