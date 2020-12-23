package cn.academy.ability.vanilla.meltdowner.skill

import java.util.function.Consumer

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.render.particle.MdParticleFactory
import cn.academy.client.render.util.ACRenderingHelper
import cn.academy.client.sound.ACSounds
import cn.academy.client.sound.FollowEntitySound
import cn.academy.entity.EntityMDRay
import cn.academy.util.RangedRayDamage
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{MathUtils, Raytrace, VecUtils}
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.{RayTraceResult, Vec3d}
import cn.lambdalib2.util.MathUtils.lerpf
import cn.lambdalib2.util.RandUtils.ranged
import cn.lambdalib2.util.RandUtils.rangei
import net.minecraft.util.SoundCategory

/**
  * @author WeAthFolD
  */
object Meltdowner extends Skill("meltdowner", 3) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = {
    activateSingleKey(rt, keyid, p => new MDContext(p))
  }

}

object MDContext {

  final val MSG_PERFORM = "perform"
  final val MSG_REFLECTED = "reflect"
  final val TICKS_MIN: Int = 20
  final val TICKS_MAX: Int = 40
  final val TICKS_TOLE: Int = 100

}

import cn.academy.ability.api.AbilityAPIExt._
import MDContext._

class MDContext(player: EntityPlayer) extends Context(player, Meltdowner) {

  private var ticks: Int = 0
  final private val exp: Float = ctx.getSkillExp
  final private val tickConsumption: Float = lerpf(10, 15, exp)

  private var overloadKeep = 0f

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    val overload = lerpf(200, 170, exp)
    ctx.consume(overload, 0)
    overloadKeep = ctx.cpData.getOverload
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_keyUp() {
    if(ticks >= MDContext.TICKS_MIN) sendToServer(MSG_PERFORM)
    else terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_keyAbort() {
    terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT, Side.SERVER))
  private def g_tick() {
    ticks += 1
    if(!isRemote) {
      if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
      if(!ctx.consume(0, tickConsumption) || ticks > MDContext.TICKS_TOLE) terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def s_perform() {
    val ct: Int = toChargeTicks
    val length: Array[Double] = Array[Double](30) // for lambda mod
    val rrd: RangedRayDamage = new RangedRayDamage.Reflectible(ctx, lerpf(2, 3, exp), getEnergy(ct), new Consumer[Entity] {
      override def accept(reflector: Entity): Unit = {
        length.update(0, Math.min(length.apply(0), reflector.getDistanceSq(ctx.player)))

        s_reflected(reflector)
        sendToClient(MSG_REFLECTED, reflector)
      }
    })
    rrd.startDamage = getDamage(ct)
    rrd.perform()
    ctx.addSkillExp(getExpIncr(ct))
    ctx.setCooldown(getCooldown(ct))
    sendToClient(MSG_PERFORM, ct.asInstanceOf[AnyRef], length.apply(0).asInstanceOf[AnyRef])
    terminate()
  }

  private def s_reflected(reflector: Entity) {
    val result: RayTraceResult = Raytrace.traceLiving(reflector, 10)
    if(result != null && (result.typeOfHit eq RayTraceResult.Type.ENTITY)) ctx.attack(result.entityHit, 0.5f * lerpf(20, 50, exp))
  }

  private def timeRate(ct: Int): Float = MathUtils.lerpf(0.8f, 1.2f, (ct - 20.0f) / 20.0f)

  private def getEnergy(ct: Int): Float = timeRate(ct) * MathUtils.lerpf(300, 700, exp)

  private def getDamage(ct: Int): Float = timeRate(ct) * MathUtils.lerpf(18, 50, exp)

  private def getCooldown(ct: Int): Int = (timeRate(ct) * 20 * MathUtils.lerpf(15, 7, exp)).toInt

  private def getExpIncr(ct: Int): Float = timeRate(ct) * 0.002f

  private def toChargeTicks: Int = Math.min(ticks, MDContext.TICKS_MAX)

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[MDContext])
class MDContextC(par: MDContext) extends ClientContext(par) {

  private var ticks: Int = 0

  private var sound: FollowEntitySound = _

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_perform(ct: Int, length: Double) {
    val ray: EntityMDRay = new EntityMDRay(ctx.player, length)
    ACSounds.playClient(ctx.player, "md.meltdowner", SoundCategory.PLAYERS, 0.5f)
    world.spawnEntity(ray)
  }

  @Listener(channel=MSG_REFLECTED, side=Array(Side.CLIENT))
  private def c_reflected(reflector: Entity) {
    val playerLook: Vec3d = ctx.player.getLookVec.normalize
    val distance: Double = VecUtils.entityHeadPos(ctx.player).distanceTo(VecUtils.entityHeadPos(reflector))
    val spawnPos: Vec3d = VecUtils.add(VecUtils.entityHeadPos(ctx.player), VecUtils.multiply(playerLook, distance))
    val ray: EntityMDRay = new EntityMDRay(ctx.player, 10)
    ray.setPosition(spawnPos.x, spawnPos.y, spawnPos.z)
    world.spawnEntity(ray)
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_terminate() {
    if(isLocal) ctx.player.capabilities.setPlayerWalkSpeed(0.1f)
    sound.stop()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_start() {
    sound = new FollowEntitySound(ctx.player, "md.md_charge", SoundCategory.AMBIENT).setVolume(1.0f)
    ACSounds.playClient(sound)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_tick() {
    ticks += 1
    if(isLocal) ctx.player.capabilities.setPlayerWalkSpeed(0.1f - ticks * 0.001f)
    // Particles surrounding player
    for(count <- rangei(2, 3) to 0) {
      val r: Double = ranged(0.7, 1)
      val theta: Double = ranged(0, Math.PI * 2)
      val h: Double = ranged(-1.2, 0)
      val pos: Vec3d = VecUtils.add(new Vec3d(ctx.player.posX, ctx.player.posY + (if(ACRenderingHelper.isThePlayer(ctx.player)) 0
      else 1.6), ctx.player.posZ), new Vec3d(r * Math.sin(theta), h, r * Math.cos(theta)))
      val vel: Vec3d = new Vec3d(ranged(-.03, .03), ranged(.01, .05), ranged(-.03, .03))
      world.spawnEntity(MdParticleFactory.INSTANCE.next(world, pos, vel))
    }
  }

}