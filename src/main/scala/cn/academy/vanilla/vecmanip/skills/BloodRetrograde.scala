package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ClientRuntime, Context}
import cn.academy.vanilla.generic.entity.EntityBloodSplash
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.RandUtils
import cn.lambdalib.util.mc._
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.DamageSource

object BloodRetrograde extends Skill("blood_retro", 4) {

  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new BloodRetroContext(p))

}

import cn.academy.ability.api.AbilityAPIExt._

private object BloodRetroContext {
  final val MSG_PERFORM = "perform"
}

class BloodRetroContext(p: EntityPlayer) extends Context(p) {
  import BloodRetroContext._
  import cn.lambdalib.util.mc.MCExtender._
  import RandUtils._
  import cn.lambdalib.util.generic.MathUtils._

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

  @SideOnly(Side.CLIENT)
  @Listener(channel=MSG_PERFORM, side=Array(Side.CLIENT))
  def c_perform(targ: EntityLivingBase) = {
    (0 until rangei(6, 10)).foreach(_ => {
      val splash = new EntityBloodSplash(world)
      splash.setSize(rangef(1.4f, 1.8f))

      splash.setPos(targ.position +
        Vec3(ranged(-1, 1) * targ.width, ranged(0, 1) * targ.height, ranged(-1, 1) * targ.width) +
        player.lookVector * 0.2)

      world.spawnEntityInWorld(splash)
    })
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  def s_perform(targ: EntityLivingBase) = {
    if (consume()) {
      sendToClient(MSG_PERFORM, targ)
      targ.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)
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

  private def consume() = true

  private def damage = 10

}