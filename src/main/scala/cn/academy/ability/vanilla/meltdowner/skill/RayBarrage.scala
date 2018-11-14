package cn.academy.ability.vanilla.meltdowner.skill

import java.util.function.Predicate

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.entity.{EntityBarrageRayPre, EntityMdRayBarrage, EntitySilbarn}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.entityx.event.CollideEvent
import cn.lambdalib2.util._
import cn.lambdalib2.util.VecUtils._
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.{AxisAlignedBB, MathHelper, RayTraceResult, Vec3d}

/**
  * @author WeAthFolD, KSkun
  */
object RayBarrage extends Skill("ray_barrage", 4) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new RBContext(p))

}

object RBContext {

  final val MSG_START = "start"
  final val MSG_SYNC_SILBARN = "sync_silbarn"
  final val MSG_EXECUTE = "execute"

  final val MSG_EFFECT_PRERAY = "effect_preray"
  final val MSG_EFFECT_BARRAGE = "effect_barrage"

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import RBContext._

class RBContext(p: EntityPlayer) extends Context(p, RayBarrage) {

  private val DISPLAY_RAY_DIST: Double = 20
  private val RAY_DIST: Double = 20

  private var hit: Boolean = false
  private var silbarn: EntitySilbarn = _

  private def getPlainDamage(exp: Float): Float = lerpf(25, 60, exp)

  private def getScatteredDamage(exp: Float): Float = lerpf(10, 18, exp)

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_onKeyDown() = {
    sendToServer(MSG_START)
  }

  @Listener(channel=MSG_SYNC_SILBARN, side=Array(Side.CLIENT))
  private def c_sync_silbarn(_silbarn: EntitySilbarn) = {
    silbarn = _silbarn
  }

  @Listener(channel=MSG_START, side=Array(Side.SERVER))
  private def s_consume() = {
    val exp: Float = ctx.getSkillExp

    val pos: RayTraceResult = Raytrace.traceLiving(player, DISPLAY_RAY_DIST)
    if(pos != null && pos.entityHit.isInstanceOf[EntitySilbarn] && !pos.entityHit.asInstanceOf[EntitySilbarn].isHit) {
      hit = true
      silbarn = pos.entityHit.asInstanceOf[EntitySilbarn]
    }

    sendToClient(MSG_SYNC_SILBARN, silbarn)

    val cp: Float = lerpf(450, 380, exp)
    val overload: Float = lerpf(300, 140, exp)
    if(!ctx.consume(overload, cp)) terminate()
    sendToSelf(MSG_EXECUTE)
  }

  @Listener(channel=MSG_EXECUTE, side=Array(Side.SERVER))
  private def s_execute(): Unit = {
    val exp: Float = ctx.getSkillExp

    var tx: Double = .0
    var ty: Double = .0
    var tz: Double = .0
    if(hit) {
      if(silbarn == null) return
      tx = silbarn.posX
      ty = silbarn.posY
      tz = silbarn.posZ
      // Post the collide event to make it break. Might a bit hacking
      silbarn.postEvent(new CollideEvent(new RayTraceResult(silbarn)))
      sendToClient(MSG_EFFECT_BARRAGE, silbarn)
      // Do the damage
      val range: Float = 55
      
      val yaw: Float = player.rotationYaw
      val pitch: Float = player.rotationPitch
      
      val minYaw: Float = yaw - range / 2
      val maxYaw: Float = yaw + range / 2
      
      val minPitch: Float = pitch - range
      val maxPitch: Float = pitch + range
      
      val selector: Predicate[Entity] = EntitySelectors.exclude(silbarn, player)

      val pos = player.getPositionVector
      val mo = player.getLookVec
      
      val v0: Vec3d = player.getPositionVector
      val v1: Vec3d = add(multiply(mo.rotateYaw(minYaw).rotatePitch(minPitch),RAY_DIST), pos)
      val v2: Vec3d = add(multiply(mo.rotateYaw(minYaw).rotatePitch(maxPitch),RAY_DIST), pos)
      val v3: Vec3d = add(multiply(mo.rotateYaw(maxYaw).rotatePitch(maxPitch),RAY_DIST), pos)
      val v4: Vec3d = add(multiply(mo.rotateYaw(maxYaw).rotatePitch(minPitch),RAY_DIST), pos)
      
      val aabb: AxisAlignedBB = WorldUtils.minimumBounds(v0, v1, v2, v3, v4)
      val list: java.util.List[Entity] = WorldUtils.getEntities(player.world, aabb, selector)
      
      import scala.collection.JavaConversions._
      
      for (e <- list) {
        // Double check whether the entity is within range.
        val dx: Double = e.posX - player.posX
        val dy: Double = (e.posY + e.getEyeHeight) - (player.posY + player.getEyeHeight)
        val dz: Double = e.posZ - player.posZ

        val eyaw: Float = -(Math.atan2(dx, dz) * 180.0D / 3.141592653589793D).toFloat
        val epitch: Float = -(Math.atan2(dy, Math.sqrt(dz * dz + dz * dz)) * 180.0D / 3.141592653589793D).toFloat
        if(MathUtils.angleYawinRange(minYaw, maxYaw, eyaw) && (minPitch <= epitch && epitch <= maxPitch)) 
          MDDamageHelper.attack(ctx, e, getScatteredDamage(exp))
      }
    } else {
      val pres: org.apache.commons.lang3.tuple.Pair[Vec3d, RayTraceResult] = Raytrace.getLookingPos(player, RAY_DIST)
      val pos: Vec3d = pres.getLeft
      val result: RayTraceResult = pres.getRight
      
      tx = pos.x
      ty = pos.y
      tz = pos.z
      
      if(result != null && result.entityHit != null)
        MDDamageHelper.attack(ctx, result.entityHit, getPlainDamage(exp))
    }
    
    sendToClient(MSG_EFFECT_PRERAY, player.posX.asInstanceOf[AnyRef], player.posY.asInstanceOf[AnyRef],
      player.posZ.asInstanceOf[AnyRef], tx.asInstanceOf[AnyRef], ty.asInstanceOf[AnyRef], tz.asInstanceOf[AnyRef],
      hit.asInstanceOf[AnyRef])

    ctx.setCooldown(lerpf(100, 40, exp).toInt)
    ctx.addSkillExp(.005f)
    terminate()
  }

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[RBContext])
class RBContextC(par: RBContext) extends ClientContext(par) {
  
  @Listener(channel=MSG_EFFECT_PRERAY, side=Array(Side.CLIENT))
  private def c_spawnPreRay(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double, hit: Boolean) = {
    val raySmall: EntityBarrageRayPre = new EntityBarrageRayPre(player.world, hit)
    raySmall.setFromTo(x0, y0 + 1.6, z0, x1, y1, z1)
    player.world.spawnEntity(raySmall)
  }
  
  @Listener(channel=MSG_EFFECT_BARRAGE, side=Array(Side.CLIENT))
  private def c_spawnBarrage(silbarn: EntitySilbarn) = player.world.spawnEntity(new EntityMdRayBarrage(player.world,
    silbarn.posX, silbarn.posY, silbarn.posZ, player.rotationYaw, player.rotationPitch))
  
}