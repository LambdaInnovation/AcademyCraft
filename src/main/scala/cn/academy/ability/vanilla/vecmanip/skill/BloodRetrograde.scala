package cn.academy.ability.vanilla.vecmanip.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.client.sound.ACSounds
import cn.academy.entity.EntityBloodSplash
import cn.academy.ability.vanilla.generic.client.effect.BloodSprayEffect
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util._
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.{RayTraceResult, Vec3d}
import net.minecraft.util.SoundCategory
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

object BloodRetrograde extends Skill("blood_retro", 4) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new BloodRetroContext(p))

}

private object BloodRetroContext {
  final val MSG_PERFORM = "perform"
}

import BloodRetroContext._
import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.AbilityPipeline._
import cn.academy.ability.api.AbilityAPIExt._

class BloodRetroContext(p: EntityPlayer) extends Context(p, BloodRetrograde) {

  var tick = 0

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    val trace: RayTraceResult = Raytrace.traceLiving(p, 2)
    trace.typeOfHit match {
      case RayTraceResult.Type.ENTITY =>
        sendToServer(MSG_PERFORM, trace.entityHit.asInstanceOf[EntityLivingBase])
      case _ =>
        terminate()
    }
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform(targ: EntityLivingBase) = {
    if (consume()) {
      ctx.setCooldown(lerpf(90, 40, ctx.getSkillExp).toInt)
      sendToClient(MSG_PERFORM, targ)
      ctx.attack(targ, damage)
      ctx.addSkillExp(0.002f)
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
    val overload = lerpf(55, 40, ctx.getSkillExp)
    val consumption = lerpf(280, 350, ctx.getSkillExp)

    ctx.consume(overload, consumption)
  }

  private val damage = lerpf(30, 60, ctx.getSkillExp)

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[BloodRetroContext])
class BloodRetroContextC(par: BloodRetroContext) extends ClientContext(par) {

  import cn.lambdalib2.util.RandUtils._
  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  private def c_perform(targ: EntityLivingBase) = {
    (0 until rangei(6, 10)).foreach(_ => {
      val splash = new EntityBloodSplash(world)
      splash.setSize(rangef(1.4f, 1.8f))
      val dv = new Vec3d(ranged(-1, 1) * targ.width, ranged(0, 1) * targ.height, ranged(-1, 1) * targ.width)
      splash.setPosition(targ.posX + dv.x + player.getLookVec.x * 0.2,
        targ.posY + dv.y + player.getLookVec.y * 0.2,
        targ.posZ + dv.z + player.getLookVec.z * 0.2)

      world.spawnEntity(splash)
    })

    val headPos = targ.getPositionVector.add(0, targ.height * 0.6, 0)

    List(0, 30, 45, 60, 80, -30, -45, -60, -80)
      .map(angle => new EntityLook(player.rotationYawHead + rangef(-20, 20), angle).toVec3)
      .map(look => Raytrace.perform(world,
        new Vec3d(headPos.x - look.x * 0.5, headPos.y - look.y * 0.5, headPos.z - look.z * 0.5),
        new Vec3d(headPos.x + look.x * 5, headPos.y + look.y * 5, headPos.z + look.z * 5),
        EntitySelectors.nothing, BlockSelectors.filNormal))
      .filter(r => r != null && r.typeOfHit == RayTraceResult.Type.BLOCK)
      .foreach(r=> {
        (0 until rangei(2, 3)).foreach(_ => {
          val spray = new BloodSprayEffect(world, r.getBlockPos, r.sideHit.getIndex)
          world.spawnEntity(spray)
        })
      })
    ACSounds.playClient(player, "vecmanip.blood_retro", SoundCategory.AMBIENT,1F)
  }

}