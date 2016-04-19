package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.core.client.sound.ACSounds
import cn.academy.vanilla.generic.client.effect.BloodSprayEffect
import cn.academy.vanilla.generic.entity.EntityBloodSplash
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.generic.RandUtils._
import cn.lambdalib.util.mc._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.DamageSource

object BloodRetrograde extends Skill("blood_retro", 4) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new BloodRetroContext(p))

}

private object BloodRetroContext {
  final val MSG_PERFORM = "perform"
}

import BloodRetroContext._
import cn.lambdalib.util.mc.MCExtender._
import cn.lambdalib.util.generic.MathUtils._
import cn.academy.ability.api.AbilityPipeline._
import cn.academy.ability.api.AbilityAPIExt._

class BloodRetroContext(p: EntityPlayer) extends Context(p) {

  implicit val aData_ = aData()
  implicit val skill_ = BloodRetrograde
  implicit val player_ = p

  var tick = 0

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    val trace: TraceResult = Raytrace.traceLiving(player, 2, EntitySelectors.living)
    trace match {
      case EntityResult(ent) =>
        sendToServer(MSG_PERFORM, ent)
      case _ =>
        terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform(targ: EntityLivingBase) = {
    if (consume()) {
      addSkillCooldown(lerpf(90, 40, skillExp).toInt)
      sendToClient(MSG_PERFORM, targ)
      attack(player, BloodRetrograde, targ, damage)
      addSkillExp(0.002f)
    }

    terminate()
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  def l_tick() = if (isLocal) {
    tick += 1

    player.capabilities.setPlayerWalkSpeed(lerpf(0.1f, 0.007f, clampf(0, 1, tick / 20.0f)))

    if (tick >= 30.0f) {
      l_keyUp()
    }
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.CLIENT))
  def l_terminate() = if (isLocal) {
    player.capabilities.setPlayerWalkSpeed(0.1f)
  }

  private def consume() = {
    val overload = lerpf(55, 40, skillExp)
    val consumption = lerpf(280, 350, skillExp)

    cpData.perform(overload, consumption)
  }

  private val damage = lerpf(30, 60, skillExp)

}

@Registrant
@RegClientContext(classOf[BloodRetroContext])
class BloodRetroContextC(par: BloodRetroContext) extends ClientContext(par) {

  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_perform(targ: EntityLivingBase) = {
    (0 until rangei(6, 10)).foreach(_ => {
      val splash = new EntityBloodSplash(world)
      splash.setSize(rangef(1.4f, 1.8f))

      splash.setPos(targ.position +
        Vec3(ranged(-1, 1) * targ.width, ranged(0, 1) * targ.height, ranged(-1, 1) * targ.width) +
        player.lookVector * 0.2)

      world.spawnEntityInWorld(splash)
    })

    val headPos = targ.position
    headPos.yCoord += targ.height * 0.6

    List(0, 30, 45, 60, 80, -30, -45, -60, -80)
      .map(angle => new EntityLook(player.rotationYawHead + rangef(-20, 20), angle).toVec3)
      .map(look => implicitly[TraceResult](Raytrace.perform(world, headPos - look * 0.5, headPos + look * 5,
        EntitySelectors.nothing, BlockSelectors.filNormal)))
      .foreach {
        case BlockResult((x, y, z), side) =>
          (0 until rangei(2, 3)).foreach(_ => {
            val spray = new BloodSprayEffect(world, x, y, z, side)
            world.spawnEntityInWorld(spray)
          })
        case _ =>
      }

    ACSounds.playClient(player, "vecmanip.blood_retro", 1f)
  }

}