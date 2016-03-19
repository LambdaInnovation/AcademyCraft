package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{Context, ClientRuntime}
import cn.academy.core.client.sound.ACSounds
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.mc._
import cn.lambdalib.vis.animation.presets.CompTransformAnim
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

object DirectedShock extends Skill("dir_shock", 1) {

  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ShockContext(p))

}


private object ShockContext {
  final val MSG_PERFORM = "perform"
  final val MSG_GENERATE_EFFECT = "gen_eff"
}

class ShockContext(p: EntityPlayer) extends Context(p) {
  import cn.academy.ability.api.AbilityAPIExt._
  import ShockContext._
  import MCExtender._
  import cn.academy.vanilla.vecmanip.client.effect.AnimPresets._
  import cn.academy.ability.api.AbilityPipeline._
  import MathUtils._

  implicit val skill_ = DirectedShock
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

    if (consume()) {
      val trace: TraceResult = Raytrace.traceLiving(player, 3, EntitySelectors.living)
      trace match {
        case EntityResult(entity) =>
          attack(player, DirectedShock, entity, damage)
          knockback(entity)

          sendToClient(MSG_GENERATE_EFFECT, entity)

          val delta = (entity.position - player.position).normalize() * 0.24
          entity.setVel(entity.velocity + delta)

          addSkillExp(0.0035f)
        case _ =>
          addSkillExp(0.0010f)
      }
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

    addSkillCooldown(lerpf(60, 20, skillExp).toInt)
  }

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def c_effect(ent: Entity) = {
    knockback(ent)

    ACSounds.playClient(player, "vecmanip.directed_shock", 0.5f)
  }

  private def consume() = {
    val cp = lerpf(50, 100, skillExp)
    val overload = lerpf(18, 12, skillExp)

    cpData.perform(overload, cp)
  }
  private def damage = lerpf(6, 12, skillExp)
  private def knockback(targ: Entity) = if (skillExp >= 0.25f) {
    var delta = player.headPosition - targ.headPosition
    delta = delta.normalize()
    delta.yCoord = -0.6f
    delta = delta.normalize()

    targ.setPosition(targ.posX, targ.posY + 0.1, targ.posZ)
    targ.setVel(delta * -0.7f)
  }

}