package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.sound.ACSounds
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.helper.GameTimer
import cn.lambdalib.util.mc._
import cn.lambdalib.vis.animation.presets.CompTransformAnim
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

object DirectedShock extends Skill("dir_shock", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ShockContext(p))

}

private object ShockContext {
  final val MSG_PERFORM = "perform"
  final val MSG_GENERATE_EFFECT = "gen_eff"
}

import cn.academy.ability.api.AbilityAPIExt._
import ShockContext._
import MCExtender._
import cn.academy.vanilla.vecmanip.client.effect.AnimPresets._
import cn.academy.ability.api.AbilityPipeline._
import MathUtils._

class ShockContext(p: EntityPlayer) extends Context(p) {

  private implicit val skill_ = DirectedShock
  private implicit val aData_ = aData
  private implicit val player_ = p

  private val MIN_TICKS = 6
  private val MAX_ACCEPTED_TICKS = 50
  private val MAX_TOLERANT_TICKS = 200
  private val PUNCH_ANIM_TICKS = 6

  private var ticker = 0

  private var punched = false
  private var punchTicker = 0

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

          addSkillCooldown(lerpf(60, 20, skillExp).toInt)
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

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def c_effect(ent: Entity) = {
    knockback(ent)
    punched = true
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

@Registrant
@RegClientContext(classOf[ShockContext])
class ShockContextC(par: ShockContext) extends ClientContext(par) {

  var handEffect: HandRenderer = _

  var anim: CompTransformAnim = _

  var timeProvider: () => Double = null

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def l_effect() = if (isLocal) {
    val init = GameTimer.getTime
    timeProvider = () => {
      val dt = GameTimer.getTime - init
      dt / 300.0
    }

    anim = createPunchAnim()
    anim.perform(0)
  }

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def c_effect() = {
    ACSounds.playClient(player, "vecmanip.directed_shock", 0.5f)
  }

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

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_handEffectTerminate() = if (isLocal) {
    HandRenderInterrupter(player).stopInterrupt(handEffect)
  }

}