package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ContextManager, Context, ClientRuntime}
import cn.academy.ability.api.event.ReflectEvent
import cn.academy.vanilla.vecmanip.client.effect.WaveEffect
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.mc.Vec3
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge

object VecReflection extends Skill("vec_reflection", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new VecReflectionContext(p))

}

object VecReflectionContext {
  final val MSG_EFFECT = "effect"
}

class VecReflectionContext(p: EntityPlayer) extends Context(p) {
  import VecReflectionContext._
  import cn.lambdalib.util.generic.RandUtils._
  import cn.academy.ability.api.AbilityAPIExt._
  import cn.lambdalib.util.mc.MCExtender._
  import net.minecraft.util.{Vec3 => MCVec3}

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER, Side.CLIENT))
  def g_makeAlive() = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    // terminate()
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  def l_keyAbort() = {
    terminate()
  }

  @Listener(channel=MSG_TERMINATED, side=Array(Side.SERVER, Side.CLIENT))
  def g_terminate() = {
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  def reflectEffect(point: MCVec3) = {
    val eff = new WaveEffect(world, 2, 1.1)
    eff.setPos(point)
    eff.rotationYaw = player.rotationYawHead
    eff.rotationPitch = player.rotationPitch

    world.spawnEntityInWorld(eff)
  }

  @SubscribeEvent
  def onReflect(evt: ReflectEvent) = {
    if (evt.target.equals(player)) {
      evt.setCanceled(true)

      val dpos = evt.player.headPosition - player.headPosition
      sendToClient(MSG_EFFECT, player.position + Vec3(0, ranged(0.4, 1.3), 0) + dpos.normalize() * 0.5)
    }
  }

}
