package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{Context, ClientRuntime}
import cn.academy.vanilla.vecmanip.client.effect.DestMarkEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc._
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.entity.player.EntityPlayer

object VecAccel extends Skill("vec_accel", 2) {

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

  var ticker = 0
  var estimateHit = Vec3(0, 0, 0)
  if (isLocal) updateEstimate()

  var destMark: DestMarkEffect = null
  var canPerform = true

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    sendToSelf(MSG_PERFORM)
    sendToServer(MSG_PERFORM)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  def l_begin() = if (isLocal) {
    destMark = new DestMarkEffect(world) {
      override def canPerform = VecAccelContext.this.canPerform
    }
    destMark.setPos(estimateHit)
    world.spawnEntityInWorld(destMark)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_terminate() = if (isLocal) {
    destMark.setDead()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tickHandler() = if (isLocal) {
    ticker += 1

    updateEstimate()

    destMark.setPos(estimateHit)

    updateCanPerform()
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def l_perform() = if (isLocal) {
    if (canPerform && consume()) {
      player.setVel(player.lookVector * speed)
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform() = {
    if (canPerform && consume()) {
      player.fallDistance = 0
    }
    terminate()
  }

  private def updateEstimate() = {
    val playerPos = player.position

    val tracePair = Raytrace.getLookingPos(player, 10, EntitySelectors.nothing)
    val dest = tracePair.getLeft
    val distance = (dest - playerPos).lengthVector

    val velocity = player.lookVector * speed
    val vhoriz = sqrt(velocity.z * velocity.z + velocity.x * velocity.x)
    val t = if (vhoriz < 1e-3) 10 else min(20, distance / vhoriz) // Limit the value, in case vhoriz is too small

    val dy = velocity.y * t + 0.5 * PLAYER_ACCEL * (t * t)
    val realHorizDist = INV_LN_A * (math.exp(LN_A * t) - 1) * vhoriz // Recalculate the horizontal offset. Takes damping into account
    estimateHit.set(playerPos + Vec3(velocity.x, 0, velocity.z) * realHorizDist)
    estimateHit.yCoord += dy
  }

  private def speed = {
    val prog = lerp(0.4, 1, clampd(0, 1, ticker.toDouble / MAX_CHARGE))
    sin(prog) * MAX_VELOCITY
  }

  private def consume() = true

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
