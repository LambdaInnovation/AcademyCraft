package cn.academy.ability.vanilla.vecmanip.skill

import cn.academy.ability.Skill
import cn.academy.ability.context._
import cn.academy.client.sound.ACSounds
import cn.academy.ability.vanilla.vecmanip.client.effect.ParabolaEffect
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util._
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.{RayTraceResult, Vec3d}

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
import cn.lambdalib2.util.MathUtils._
import Math._

class VecAccelContext(p: EntityPlayer) extends Context(p, VecAccel) with IConsumptionProvider {

  override def getConsumptionHint: Float = consumption

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
      VecUtils.setMotion(player, initSpeed())
      if(player.getLowestRidingEntity==null)player.dismountRidingEntity()
      ctx.setCooldown(lerpf(80, 50, ctx.getSkillExp).toInt)

      sendToServer(MSG_PERFORM)
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    consume()
    player.fallDistance = 0
    ctx.addSkillExp(0.002f)

    sendToClient(MSG_PERFORM)
    terminate()
  }

  def initSpeed(partialTicks: Float = 0.0f) = {
    val look = new EntityLook(
      MathUtils.lerpf(player.prevRotationYaw, player.rotationYaw, partialTicks),
      MathUtils.lerpf(player.prevRotationPitch, player.rotationPitch, partialTicks) - 10).toVec3
    VecUtils.multiply(look, speed)
  }

  private def speed = {
    val prog = lerp(0.4, 1, clampd(0, 1, ticker.toDouble / MAX_CHARGE))
    sin(prog) * MAX_VELOCITY
  }

  private def consume() = {
    val cp = consumption
    val overload = lerpf(30, 15, ctx.getSkillExp)

    ctx.consume(overload, cp)
  }

  private val consumption = lerpf(120, 80, ctx.getSkillExp)

  private def updateCanPerform() = canPerform = ignoreGroundChecking || checkGround

  private val ignoreGroundChecking = ctx.getSkillExp > 0.5f

  private def checkGround = {
    val p0 = player.getPositionVector
    val p1 = new Vec3d(p0.x, p0.y - 2,p0.z)

    val traceResult: RayTraceResult = Raytrace.perform(world, p0, p1, EntitySelectors.nothing, BlockSelectors.filNothing)
    traceResult != null && traceResult.typeOfHit==RayTraceResult.Type.BLOCK
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[VecAccelContext])
class VecAccelContextC(par: VecAccelContext) extends ClientContext(par) {

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_makeAlive() = if (isLocal) {
    world().spawnEntity(new ParabolaEffect(par))
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform() = {
    ACSounds.playClient(player, "vecmanip.vec_accel", SoundCategory.AMBIENT, 0.35f)
  }

}