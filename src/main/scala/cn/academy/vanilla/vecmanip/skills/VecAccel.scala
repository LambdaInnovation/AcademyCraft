package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context._
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.vecmanip.client.effect.ParabolaEffect
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.mc._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer

object VecAccel extends Skill("vec_accel", 2) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new VecAccelContext(p))

}

object VecAccelContext {
  final val MSG_PERFORM = "perform"

  final val MAX_VELOCITY = 2.5
  final val MAX_CHARGE = 20
  final val PLAYER_ACCEL = -0.08
  final val DAMPING = 0.9
  final val LN_A = math.log(DAMPING)
}

import cn.academy.ability.api.AbilityAPIExt._
import VecAccelContext._
import cn.lambdalib.util.mc.MCExtender._
import cn.lambdalib.util.generic.MathUtils._
import Math._

class VecAccelContext(p: EntityPlayer) extends Context(p) with IConsumptionProvider {

  override def getConsumptionHint: Float = consumption

  private implicit val skill_ = VecAccel
  private implicit val aData_ = aData
  private implicit val player_ = player

  var ticker = 0

  var canPerform = true

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = l_perform()

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = terminate()

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tickHandler() = if (isLocal) {
    ticker += 1

    updateCanPerform()
  }

  def l_perform() = {
    if (canPerform && consume()) {
      player.setVel(initSpeed())
      addSkillCooldown(lerpf(25, 5, skillExp).toInt)

      sendToServer(MSG_PERFORM)
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    consume()
    player.fallDistance = 0
    addSkillExp(0.002f)

    sendToClient(MSG_PERFORM)
    terminate()
  }

  def initSpeed(partialTicks: Float = 0.0f) = {
    val look = new EntityLook(
      MathUtils.lerpf(player.prevRotationYaw, player.rotationYaw, partialTicks),
      MathUtils.lerpf(player.prevRotationPitch, player.rotationPitch, partialTicks) - 10).toVec3
    look * speed
  }

  private def speed = {
    val prog = lerp(0.4, 1, clampd(0, 1, ticker.toDouble / MAX_CHARGE))
    sin(prog) * MAX_VELOCITY
  }

  private def consume() = {
    val cp = consumption
    val overload = lerpf(40, 30, skillExp)

    cpData.perform(overload, cp)
  }

  private def consumption = lerpf(200, 160, skillExp)

  private def updateCanPerform() = canPerform = ignoreGroundChecking || checkGround

  private def ignoreGroundChecking = aData.getSkillExp(VecAccel) > 0.5f

  private def checkGround = {
    val p0 = player.position
    val p1 = p0 - Vec3(0, 2, 0)

    val traceResult: TraceResult = Raytrace.perform(world, p0, p1, EntitySelectors.nothing, BlockSelectors.filNothing)
    traceResult match {
      case BlockResult(_,_) => true
      case _ => false
    }
  }

}

@Registrant
@RegClientContext(classOf[VecAccelContext])
class VecAccelContextC(par: VecAccelContext) extends ClientContext(par) {

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_makeAlive() = if (isLocal) {
    world().spawnEntityInWorld(new ParabolaEffect(par))
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform() = {
    ACSounds.playClient(player, "vecmanip.vec_accel", 0.35f)
  }

}
