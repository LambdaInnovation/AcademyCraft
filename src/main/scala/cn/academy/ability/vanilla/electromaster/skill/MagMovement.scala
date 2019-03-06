package cn.academy.ability.vanilla.electromaster.skill

import cn.academy.AcademyCraft
import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.render.util.{ACRenderingHelper, ArcPatterns}
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.entity.EntityArc
import cn.academy.ability.vanilla.electromaster.CatElectromaster
import cn.academy.datapart.AbilityData
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{MathUtils, Raytrace}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.{BlockPos, RayTraceResult, Vec3d}
import net.minecraft.world.World

/**
  * @author WeAthFolD, KSkun
  */
object MagMovement extends Skill("mag_movement", 2) {

  final val ACCEL = 0.08d

  final val SOUND = "em.move_loop"

  def getMaxDistance(data: AbilityData) = 25

  def toTarget(aData: AbilityData, world: World, pos: RayTraceResult): Target = {
    if(pos.typeOfHit == RayTraceResult.Type.BLOCK) {
      val block = world.getBlockState(pos.getBlockPos).getBlock
      if(aData.getSkillExp(this) < 0.6f && !CatElectromaster.isMetalBlock(block)) { return null }
      if(!CatElectromaster.isWeakMetalBlock(block) && !CatElectromaster.isMetalBlock(block)) { return null }
      new PointTarget(pos.hitVec.x, pos.hitVec.y, pos.hitVec.z)
    } else {
      if(pos.entityHit != null && CatElectromaster.isEntityMetallic(pos.entityHit)) {
        return new EntityTarget(pos.entityHit)
      }
      null
    }
  }

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new MovementContext(p))

}

object MovementContext {

  final val MSG_EFFECT_START = "effect_start"
  final val MSG_EFFECT_UPDATE = "effect_update"

}

import MagMovement._
import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import MovementContext._

class MovementContext(p: EntityPlayer) extends Context(p, MagMovement) {

  private var canSpawnEffect = false

  private var mox, moy, moz: Double = 0d
  private val sx = player.posX
  private val sy = player.posY
  private val sz = player.posZ
  private var target: Target = _

  private val exp = ctx.getSkillExp
  private val cp = lerpf(15, 8, exp)
  private val overload = lerpf(60, 30, exp)

  private var overloadKeep = 0f

  private val velocity = 1d
  private def getExpIncr(distance: Double) = Math.max(0.005f, 0.0011f * distance.asInstanceOf[Float])
  private def tryAdjust(from: Double, to: Double): Double = {
    val d = to - from
    if(Math.abs(d) < ACCEL) return to
    if(d > 0) from + ACCEL else from - ACCEL
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER, Side.CLIENT))
  private def g_onStart() = {
    ctx.consume(overload, 0)
    overloadKeep = ctx.cpData.getOverload
    val aData = AbilityData.get(player)
    val result = Raytrace.traceLiving(player, getMaxDistance(aData))
    if(result.typeOfHit != RayTraceResult.Type.MISS) {
      target = toTarget(aData, player.world, result)
      if(target == null) {
        terminate()
      } else {
        canSpawnEffect = true
      }
    } else {
      terminate()
    }
  }

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.SERVER))
  private def s_onEffectStart() = {
    sendToClient(MSG_EFFECT_START)
  }

  @Listener(channel=MSG_EFFECT_UPDATE, side=Array(Side.SERVER))
  private def s_onEffectStart(Vec3d: Vec3d) = {
    sendToClient(MSG_EFFECT_UPDATE, Vec3d)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_onTick() = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    if(canSpawnEffect) {
      sendToServer(MSG_EFFECT_START)
      canSpawnEffect = false
    }
    if(target != null) {
      target.tick()
      sendToServer(MSG_EFFECT_UPDATE, new Vec3d(target.x, target.y, target.z))
      var dx = target.x - player.posX
      var dy = target.y - player.posY
      var dz = target.z - player.posZ

      val lastMo = MathUtils.lengthSq(player.motionX, player.motionY, player.motionZ)
      if (Math.abs(MathUtils.lengthSq(mox, moy, moz) - lastMo) > 0.5) {
        mox = player.motionX
        moy = player.motionY
        moz = player.motionZ
      }

      val mod = Math.sqrt(dx * dx + dy * dy + dz * dz) / velocity

      dx /= mod
      dy /= mod
      dz /= mod

      player.motionX = tryAdjust(mox, dx)
      mox = tryAdjust(mox, dx)
      player.motionY = tryAdjust(moy, dy)
      moy = tryAdjust(moy, dy)
      player.motionZ = tryAdjust(moz, dz)
      moz = tryAdjust(moz, dz)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    if((target != null && !target.alive()) || !ctx.consume(0, cp)) terminate()
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_onEnd() = {
    val traveledDistance = MathUtils.distance(sx, sy, sz, player.posX, player.posY, player.posZ)
    ctx.addSkillExp(getExpIncr(traveledDistance))

    player.fallDistance = 0.0f
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MovementContext])
class MovementContextC(par: MovementContext) extends ClientContext(par) {

  private var arc: EntityArc = _
  private var sound: FollowEntitySound = _

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.CLIENT))
  private def c_startEffect() = {
    arc = new EntityArc(player, ArcPatterns.thinContiniousArc)
    arc.lengthFixed = false
    arc.texWiggle = 1
    arc.showWiggle = 0.1
    arc.hideWiggle = 0.6

    player.getEntityWorld.spawnEntity(arc)

    sound = new FollowEntitySound(player, SOUND, SoundCategory.AMBIENT).setLoop()
    ACSounds.playClient(sound)
  }

  @Listener(channel=MSG_EFFECT_UPDATE, side=Array(Side.CLIENT))
  private def c_updateEffect(target: Vec3d) = {
    arc.setFromTo(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ, target.x,
      target.y, target.z)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_endEffect() = {
    if(arc != null) arc.setDead()
    if(sound != null) sound.stop()
  }

}

abstract class Target {
  var x, y, z: Double = 0d

  def tick()

  def alive(): Boolean
}

private class PointTarget(_x: Double, _y: Double, _z: Double) extends Target {

  x = _x
  y = _y
  z = _z

  override def tick() = {}

  override def alive() = true

}

private class EntityTarget(_t: Entity) extends Target {

  final val target = _t

  override def tick() = {
    x = target.posX
    y = target.posY + target.getEyeHeight
    z = target.posZ
  }

  override def alive() = !target.isDead

}

private class DummyTarget(_x: Double, _y: Double, _z: Double) extends Target {

  x = _x
  y = _y
  z = _z

  override def tick() = {}

  override def alive() = true

}