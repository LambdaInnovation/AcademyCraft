package cn.academy.ability.vanilla.vecmanip.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.advancements.ACAdvancements
import cn.academy.client.render.util.{IHandRenderer, VanillaHandRenderer}
import cn.academy.client.sound.ACSounds
import cn.academy.datapart.HandRenderOverrideData
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{EntitySelectors, GameTimer, Raytrace}
import cn.lambdalib2.vis.animation.presets.CompTransformAnim
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.{RayTraceResult, Vec3d}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

object DirectedShock extends Skill("dir_shock", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ShockContext(p))

}

private object ShockContext {
  final val MSG_PERFORM = "perform"
  final val MSG_GENERATE_EFFECT = "gen_eff"
}

import cn.academy.ability.api.AbilityAPIExt._
import cn.academy.ability.vanilla.vecmanip.skill.ShockContext._
import cn.academy.client.render.util.AnimPresets._

class ShockContext(p: EntityPlayer) extends Context(p, DirectedShock) {
  import cn.lambdalib2.util.MathUtils._
  import cn.lambdalib2.util.VecUtils._

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
    import cn.lambdalib2.util.MathUtils._
    import cn.lambdalib2.util.VecUtils._
    sendToClient(MSG_PERFORM, ticks.asInstanceOf[AnyRef])

    if (consume()) {
      val trace: RayTraceResult = Raytrace.traceLiving(player, 3, EntitySelectors.living)
      if(trace != null && trace.typeOfHit == RayTraceResult.Type.ENTITY){
        val entity = trace.entityHit
        ctx.attack(entity, damage)
        knockback(entity)
        ctx.setCooldown(lerpf(60, 20, ctx.getSkillExp).toInt)
        sendToClient(MSG_GENERATE_EFFECT, entity)
//        if (ctx.getSkillExp >= 0.5f) ACAdvancements.trigger(player, ACAdvancements.ac_milestone.ID)

        val delta = multiply(subtract(entity.getPositionVector, player.getPositionVector).normalize(), 0.24)
        entity.motionX += delta.x
        entity.motionY += delta.y
        entity.motionZ += delta.z

        ctx.addSkillExp(0.0035f)
      } else {
          ctx.addSkillExp(0.0010f)
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
    val cp = lerpf(50, 100, ctx.getSkillExp)
    val overload = lerpf(18, 12, ctx.getSkillExp)

    ctx.consume(overload, cp)
  }
  private val damage = lerpf(7, 15, ctx.getSkillExp)
  private def knockback(targ: Entity) = if (ctx.getSkillExp >= 0.25f) {
    var delta = subtract(entityHeadPos(player), entityHeadPos(targ))
    delta = delta.normalize()
    delta = new Vec3d(delta.x, delta.y-0.6f, delta.z).normalize()

    targ.setPosition(targ.posX, targ.posY + 0.1, targ.posZ)
    targ.motionX = delta.x * -0.7f
    targ.motionY = delta.y * -0.7f
    targ.motionZ = delta.y * -0.7f
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ShockContext])
class ShockContextC(par: ShockContext) extends ClientContext(par) {

  var handEffect: IHandRenderer = _

  var anim: CompTransformAnim = _

  var timeProvider: () => Double = null

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def l_effect() = if (isLocal) {
    val init = GameTimer.getTime
    timeProvider = () => {
      val dt = GameTimer.getTime - init
      dt / 0.3
    }

    anim = createPunchAnim()
    anim.perform(0)
  }

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def c_effect() = {
    ACSounds.playClient(player, "vecmanip.directed_shock", SoundCategory.AMBIENT, 0.5f)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_handEffectStart() = if (isLocal) {
    anim = createPrepareAnim()

    val init = GameTimer.getTime
    timeProvider = () => {
      val dt = GameTimer.getTime - init
      math.min(2.0, dt / 0.15)
    }

    handEffect = new IHandRenderer {
      override def renderHand(partialTicks: Float) = {
        anim.perform(timeProvider())
        VanillaHandRenderer.renderHand(partialTicks, anim.target)
      }
    }

    HandRenderOverrideData.get(player).addInterrupt(handEffect)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_handEffectTerminate() = if (isLocal) {
    HandRenderOverrideData.get(player).stopInterrupt(handEffect)
  }

}