package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{Context, ClientRuntime, SingleKeyContext}
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.helper.Motion3D
import cn.lambdalib.util.mc._
import cn.lambdalib.vis.animation.presets.CompTransformAnim
import cn.lambdalib.vis.curve.CubicCurve
import cn.lambdalib.vis.model.CompTransform
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import cn.lambdalib.util.mc.MCExtender._
import net.minecraft.util.DamageSource

object DirectedBlastwave extends Skill("dir_blast", 1) {

  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, player => new BlastwaveContext(player))
  }

}

private object BlastwaveContext {
  final val MSG_PERFORM = "perform"
  final val MSG_GENERATE_EFFECT = "effect"
}

@SideOnly(Side.CLIENT)
private object BlastwaveContextClient {

  def createPrepareAnim() = {
    val anim = new CompTransformAnim(new CompTransform)

    anim.animTransform.curveY = new CubicCurve()
    val curvey = anim.animTransform.curveY
    curvey.addPoint(0, 0)
    curvey.addPoint(0.5, 0.2)
    curvey.addPoint(1, 0.4)

    anim.animTransform.curveX  = new CubicCurve()
    val curvex = anim.animTransform.curveX
    curvex.addPoint(0, 0)
    curvex.addPoint(1, -0.02)

    anim.animTransform.curveZ = new CubicCurve()
    val curvez = anim.animTransform.curveZ
    curvez.addPoint(0, 0)
    curvez.addPoint(1, -0.05)

    anim.animRotation.curveX = new CubicCurve()
    val curverx = anim.animRotation.curveX
    curverx.addPoint(0, 0)
    curverx.addPoint(1, -20)

    // anim.animRotation.curveX.addPoint(0)
    anim
  }

  def createPunchAnim() = {
    val anim = new CompTransformAnim(new CompTransform)

    anim.animTransform.curveY = new CubicCurve()
    val curvey = anim.animTransform.curveY
    curvey.addPoint(0, 0.8)
    curvey.addPoint(0.5, 0.75)
    curvey.addPoint(1, 0)

    anim.animTransform.curveX  = new CubicCurve()
    val curvex = anim.animTransform.curveX
    curvex.addPoint(0, -0.04)
    curvex.addPoint(0.5, -0.04)
    curvex.addPoint(1, 0)

    anim.animTransform.curveZ = new CubicCurve()
    val curvez = anim.animTransform.curveZ
    curvez.addPoint(0, -0.0)
    curvez.addPoint(0.3, -0.4)
    curvez.addPoint(1, 0)

    anim.animRotation.curveX = new CubicCurve()
    val curverx = anim.animRotation.curveX
    curverx.addPoint(0, -40)
    curverx.addPoint(0.5, -45)
    curverx.addPoint(1, 0)

    anim.animRotation.curveY = new CubicCurve()
    val curvery = anim.animRotation.curveY
    curvery.addPoint(0, 0)
    curvery.addPoint(0.3, 10)
    curvery.addPoint(1, 0)

    anim
  }

}

class BlastwaveContext(p: EntityPlayer) extends Context(p) {
  import cn.academy.ability.api.AbilityAPIExt._
  import BlastwaveContext._
  import MCExtender._

  val MIN_TICKS = 10
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
      println("PunchedTerminate")
      terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform(ticks: Int) = {
    sendToClient(MSG_PERFORM, ticks.asInstanceOf[AnyRef])

    val trace: TraceResult = Raytrace.traceLiving(player, 3, EntitySelectors.living)
    trace match {
      case EntityResult(entity) =>
        // println("Attack Entity " + entity)
        entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 10)
        sendToClient(MSG_GENERATE_EFFECT, entity)

        val delta = (entity.position - player.position).normalize() * 0.24
        entity.setVel(entity.velocity + delta)
      case _ => terminate()
    }
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_handEffectStart() = if (isLocal) {
    println("MadeAlive")

    anim = BlastwaveContextClient.createPrepareAnim()

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
      val time = MathUtils.clampd(0, 2.0, ticker.toDouble / 4.0)
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
  def c_effect(target: Entity) = {
    val effectGenPoint = (target.position + player.position) * 0.5

    val effect = new WaveEffect(world, 2, 0.7)
    effect.setPos(effectGenPoint)
    effect.rotationYaw = player.rotationYaw
    effect.rotationPitch = player.rotationPitch

    world.spawnEntityInWorld(effect)
  }

  @Listener(channel=MSG_GENERATE_EFFECT, side=Array(Side.CLIENT))
  def l_effect() = if (isLocal) {
    punched = true

    anim = BlastwaveContextClient.createPunchAnim()
    anim.perform(0)
  }

}