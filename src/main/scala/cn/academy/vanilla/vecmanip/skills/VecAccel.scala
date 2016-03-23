package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{Context, ClientRuntime}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc._
import cpw.mods.fml.relauncher.{SideOnly, Side}
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
  final val INV_LN_A = 1.0 / LN_A
}

class VecAccelContext(p: EntityPlayer) extends Context(p) {
  import cn.academy.ability.api.AbilityAPIExt._
  import VecAccelContext._
  import cn.lambdalib.util.mc.MCExtender._
  import cn.lambdalib.util.generic.MathUtils._
  import Math._

  private implicit val skill_ = VecAccel
  private implicit val aData_ = aData

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
      val look = new EntityLook(player.rotationYawHead, player.rotationPitch - 10)
      player.setVel(look.toVec3 * speed)

      sendToServer(MSG_PERFORM)
    }

    terminate()
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    consume()
    player.fallDistance = 0
    addSkillExp(0.002f)
  }

  private def speed = {
    val prog = lerp(0.4, 1, clampd(0, 1, ticker.toDouble / MAX_CHARGE))
    sin(prog) * MAX_VELOCITY
  }

  private def consume() = {
    val cp = lerpf(200, 160, skillExp)
    val overload = lerpf(40, 30, skillExp)

    cpData.perform(overload, cp)
  }

  private def updateCanPerform() = canPerform = ignoreGroundChecking || checkGround

  private def ignoreGroundChecking = aData.getSkillExp(VecAccel) > 0.5f

  private def checkGround = {
    val p0 = player.position
    val p1 = p0 - Vec3(0, 2, 0)

    val traceResult: TraceResult = Raytrace.perform(world, p0, p1, EntitySelectors.nothing)
    traceResult match {
      case BlockResult(_,_) => true
      case _ => false
    }
  }

}
