package cn.academy.ability.vanilla.meltdowner.skill

import java.util.function.Predicate

import cn.academy.ability.Skill
import cn.academy.ability.context._
import cn.academy.client.render.particle.MdParticleFactory
import cn.academy.client.sound.{ACSounds, FollowEntitySound}
import cn.academy.entity.EntityMdShield
import cn.lambdalib2.particle.Particle
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util._
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.math.Vec3d
import net.minecraft.util.{DamageSource, SoundCategory}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingHurtEvent

/**
  * @author WeAthFolD, KSkun
  */
object LightShield extends Skill("light_shield", 2) {

  MinecraftForge.EVENT_BUS.register(this)

  val ACTION_INTERVAL: Int = 18
  val basicSelector: Predicate[Entity] = EntitySelectors.everything

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new LSContext(p))

  @SubscribeEvent
  def onPlayerAttacked(event: LivingHurtEvent) {
    event.getEntityLiving match {
      case player: EntityPlayer =>
        val context = ContextManager.instance.find(classOf[LSContext])
        if (context.isPresent) {
          event.setAmount(context.get().handleAttacked(event.getSource, event.getAmount))
            if (event.getAmount == 0) event.setCanceled(true)
        }
      case _ =>
    }
  }

}

object LSContext {

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import scala.collection.JavaConversions._
import LightShield._

class LSContext(p: EntityPlayer) extends Context(p, LightShield) {

  private var ticks: Int = 0
  private var lastAbsorb: Int = -1 // The tick last the shield absorbed damage.
  private val exp: Float = ctx.getSkillExp

  private final val MAX_TIME = lerpf(120, 180, exp)
  private def getCooldown(ct: Int): Int = lerpf(2 * ct, ct, exp).toInt

  private var overloadKeep = 0f

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def l_onEnd() = {
    terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def l_onAbort() = {
    terminate()
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_madeAlive() = {
    val overload: Float = lerpf(110, 60, exp)
    ctx.consume(overload, 0)
    overloadKeep = ctx.cpData.getOverload
  }
  
  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_tick() = {
    if(ctx.cpData.getOverload < overloadKeep) ctx.cpData.setOverload(overloadKeep)
    ticks += 1
    if(ticks > MAX_TIME) terminate()

    val cp: Float = lerpf(9, 4, exp)
    if (!ctx.consume(0, cp)) terminate()
    ctx.addSkillExp(1e-6f)

    // Find the entities that are 'colliding' with the shield.
    val candidates: java.util.List[Entity] = WorldUtils.getEntities(player, 3, basicSelector.and(new Predicate[Entity] {
      override def test(t: Entity): Boolean = isEntityReachable(t)
    }).and(EntitySelectors.exclude(player)))
    for (e <- candidates) {
      if (e.hurtResistantTime <= 0 && ctx.consume(getAbsorbOverload, getAbsorbConsumption)) {
        MDDamageHelper.attack(ctx, e, getTouchDamage)
        ctx.addSkillExp(.001f)
      }
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER))
  private def s_onEnd() = {

    player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("slowness"), 100, 1))
    ctx.setCooldown(getCooldown(ticks))
  }

  def handleAttacked(src: DamageSource, damage: Float): Float = {
    var result = damage
    if (damage == 0 || lastAbsorb != -1 && ticks - lastAbsorb <= ACTION_INTERVAL) return damage
    val entity: Entity = src.getImmediateSource
    var perform: Boolean = false
    if (entity != null) if (isEntityReachable(entity)) perform = true
    else perform = true
    if (perform) {
      lastAbsorb = ticks
      if (ctx.consume(getAbsorbConsumption, getAbsorbOverload)) {
        val amt: Float = getAbsorbDamage
        result -= Math.min(damage, amt)
      }
    }
    ctx.addSkillExp(.001f)
    result
  }

  private def getAbsorbDamage: Float = lerpf(15, 50, exp)

  private def getTouchDamage: Float = lerpf(2, 6, exp)

  private def getAbsorbOverload: Float = lerpf(5, 3, exp)

  private def getAbsorbConsumption: Float = lerpf(50, 30, exp)

  private def isEntityReachable(e: Entity): Boolean = {
    val dx: Double = e.posX - player.posX
    //dy = e.posY - player.posY,
    val dz: Double = e.posZ - player.posZ
    val yaw: Double = -MathUtils.toDegrees(Math.atan2(dx, dz))
    Math.abs(yaw - player.rotationYaw) % 360 < 60
  }

}

import RandUtils._

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[LSContext])
class LSContextC(par: LSContext) extends ClientContext(par) {

  private var shield: EntityMdShield = _
  private var loopSound: FollowEntitySound = _

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.CLIENT))
  private def c_spawn() = {
    shield = new EntityMdShield(player)
    world.spawnEntity(shield)
    ACSounds.playClient(player, "md.shield_startup", SoundCategory.AMBIENT, 0.5f)
    loopSound = new FollowEntitySound(player, "md.shield_loop", SoundCategory.AMBIENT).setLoop()
    ACSounds.playClient(loopSound)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_update() = {
    if (RandUtils.nextFloat < 0.3f) {
      val mo = VecUtils.lookingPos(player, 1)
      val s: Double = 0.5
      mo.x += ranged(-s, s)
      mo.y += ranged(-s, s)
      mo.z += ranged(-s, s)
      val p: Particle = MdParticleFactory.INSTANCE.next(world, new Vec3d(mo.x, mo.y, mo.z),
        new Vec3d(ranged(-.02, .02), ranged(-.01, .05), ranged(-.02, .02)))
      world.spawnEntity(p)
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  private def c_end() = {
    shield.setDead()
    loopSound.stop()
  }

}