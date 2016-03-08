package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{IConsumptionProvider, Context, ClientRuntime, SingleKeyContext}
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

  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, player => new BlastwaveContext(player))
  }

}

private object BlastwaveContext {
  final val MSG_PERFORM = "perform"
  final val MSG_GENERATE_EFFECT = "effect"
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

  implicit val skill_ = DirectedBlastwave
  implicit val aData_ = aData

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
    sendToClient(MSG_PERFORM, ticks.asInstanceOf[AnyRef])

    val trace: TraceResult = Raytrace.traceLiving(player, 3, EntitySelectors.living)
    trace match {
      case EntityResult(entity) => if (consume()) {
        attack(player, DirectedShock, entity, damage)
        knockback(entity)

        sendToClient(MSG_GENERATE_EFFECT, entity)

        val delta = (entity.position - player.position).normalize() * 0.24
        entity.setVel(entity.velocity + delta)

        addSkillExp(0.0018f)
      }
      case BlockResult((x, y, z), side) => if (consume()) {
        def ran(x: Int) = (x - 3) until (x + 3)
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
              world.playSoundEffect(i + 0.5, j + 0.5, k + 0.5, block.stepSound.getBreakSound, .5f, 1f)

              if (RNG.nextFloat() < dropRate) {
                block.dropBlockAsItemWithChance(world, i, j, k, world.getBlockMetadata(i, j, k), 1.0f, 0)
              }

              world.setBlock(i, j, k, Blocks.air)

              Minecraft.getMinecraft.effectRenderer.addBlockDestroyEffects(i, j, k, block, meta)
            }
          }
        }

        sendToClient(MSG_GENERATE_EFFECT_BLOCKS, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z))
        addSkillExp(0.0023f)
      }
      case _ =>
    }

    terminate()
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

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def l_effect() = if (isLocal) {
    punched = true

    anim = createPunchAnim()
    anim.perform(0)
  }

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def c_effect(ent: Entity) = {
    addSkillCooldown(cooldown)

    knockback(ent)
    effectAt(ent.position + util.mc.Vec3(0, ent.getEyeHeight * 0.4, 0))
  }

  @Listener(channel=MSG_GENERATE_EFFECT_BLOCKS, side=Array(Side.CLIENT))
  def c_blockEffect(x: Int, y: Int, z: Int) = {
    addSkillCooldown(cooldown)

    effectAt(util.mc.Vec3(x + 0.5, y + 0.5, z + 0.5))
  }

  private def consume() = {
    val cp = consumption
    val overload = lerpf(96, 72, skillExp)

    cpData.perform(overload, cp)
  }

  override def getConsumptionHint = consumption

  private lazy val consumption = lerpf(400, 260, skillExp)

  private lazy val breakProb = lerpf(0.1f, 0.5f, skillExp)

  private lazy val breakHardness = lerpf(3.0f, 9.0f, skillExp)

  private lazy val damage = lerpf(4, 10, skillExp)

  private lazy val dropRate = lerpf(0.4f, 0.9f, skillExp)

  private lazy val cooldown = lerpf(40, 20, skillExp).toInt

  private def knockback(targ: Entity) = {
    println("Knockback " + targ)

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