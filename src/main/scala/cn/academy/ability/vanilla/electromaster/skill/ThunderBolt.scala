package cn.academy.ability.vanilla.electromaster.skill

import java.util.function.Predicate

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.render.util.ArcPatterns
import cn.academy.client.sound.ACSounds
import cn.academy.entity.EntityArc
import cn.lambdalib2.s11n.{SerializeIncluded, SerializeNullable}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.NetworkS11nType
import cn.lambdalib2.util._
import cn.lambdalib2.util.entityx.handlers.Life
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.{RayTraceResult, Vec3d}

/**
  * @author WeAthFolD, KSkun
  */
object ThunderBolt extends Skill("thunder_bolt", 4) {

  final val RANGE = 20d
  final val AOE_RANGE = 8d

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyId: Int) = activateSingleKey(rt, keyId, p => new ThunderBoltContext(p))

}

object ThunderBoltContext {

  final val MSG_PERFORM = "perform"

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import ThunderBolt._
import ThunderBoltContext._
import scala.collection.JavaConversions._

class ThunderBoltContext(p: EntityPlayer) extends Context(p, ThunderBolt) {

  private val exp = ctx.getSkillExp
  private val aoeDamage = lerpf(6, 15, exp)
  private val damage = lerpf(10, 25, exp)
  private def getExpIncr(effective: Boolean) = if(effective) 0.005f else 0.003f

  private def consume() = {
    val overload = lerpf(50, 27, exp)
    val cp = lerp(280, 420, exp).asInstanceOf[Int]
    ctx.consume(overload, cp)
  }

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def l_onKeyDown() = {
    sendToServer(MSG_PERFORM)
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def s_perform() = {
    if(consume()) {
      val ad = getAttackData

      sendToClient(MSG_PERFORM, ad)

      var effective = false

      if(ad.target != null) {
        effective = true
        EMDamageHelper.attack(ctx, ad.target, damage)
        if(exp > 0.2 && RandUtils.ranged(0, 1) < 0.8 && ad.target.isInstanceOf[EntityLivingBase]) {
          ad.target.asInstanceOf[EntityLivingBase].addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("slowness"), 40, 3))
        }
      }

      ad.aoes.foreach((e: Entity) => {
        effective = true
        EMDamageHelper.attack(ctx, e, aoeDamage)

        if (exp > 0.2 && RandUtils.ranged(0, 1) < 0.8 && ad.target.isInstanceOf[EntityLivingBase]) {
          ad.target.asInstanceOf[EntityLivingBase].addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("slowness"), 20, 3))
        }
      })

      ctx.addSkillExp(getExpIncr(effective))

      ctx.setCooldown(lerpf(120, 50, exp).toInt)
    }
    terminate()
  }

  def getAttackData: AttackData = {
    val result = Raytrace.traceLiving(player, RANGE)
    var end: Vec3d = null
    if(result == null) {
      end = VecUtils.lookingPos(player, RANGE)
    } else {
      end = result.hitVec
      if(result.typeOfHit == RayTraceResult.Type.ENTITY) {
        end = end.add(0, result.entityHit.getEyeHeight, 0)
      }
    }

    val hitEntity = !(result == null || result.entityHit == null)
    val exclusion: Predicate[Entity] = if(!hitEntity) EntitySelectors.exclude(player) else EntitySelectors.exclude(player, result.entityHit)
    val target = if(hitEntity) result.entityHit else null
    val aoes: java.util.List[Entity] = WorldUtils.getEntities(
      player.getEntityWorld, end.x, end.y, end.z,
      AOE_RANGE, EntitySelectors.living().and(exclusion))

    val ad = new AttackData()
    ad.aoes = aoes
    ad.target = target
    ad.point = end
    ad
  }

}

import ThunderBoltContext._

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ThunderBoltContext])
class ThunderBoltContextC(par: ThunderBoltContext) extends ClientContext(par) {

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_spawnEffect(_ad: AttackData) = {
    val ad = _ad

    for(i <- 0 to 2) {
      val mainArc = new EntityArc(player, ArcPatterns.strongArc)
      mainArc.length = RANGE
      player.getEntityWorld.spawnEntity(mainArc)
      mainArc.addMotionHandler(new Life(20))
    }

    ad.aoes.foreach((e: Entity) => {
      val aoeArc = new EntityArc(player, ArcPatterns.aoeArc)
      aoeArc.lengthFixed = false
      aoeArc.setFromTo(ad.point.x, ad.point.y, ad.point.z,
        e.posX, e.posY + e.getEyeHeight, e.posZ)
      aoeArc.addMotionHandler(new Life(RandUtils.rangei(15, 25)))
      player.world.spawnEntity(aoeArc)
    })

    ACSounds.playClient(player, "em.arc_strong", SoundCategory.AMBIENT, 0.6f)
  }

}

@NetworkS11nType
class AttackData {
  @SerializeIncluded
  final var aoes: java.util.List[Entity] = _
  @SerializeIncluded
  @SerializeNullable
  final var target: Entity = _
  @SerializeIncluded
  @SerializeNullable
  final var point: Vec3d = _
}