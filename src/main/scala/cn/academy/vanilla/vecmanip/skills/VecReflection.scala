package cn.academy.vanilla.vecmanip.skills

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{ContextManager, Context, ClientRuntime}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge

object VecReflection extends Skill("vec_reflection", 4) {

  MinecraftForge.EVENT_BUS.register(this)

  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new VecReflectionContext(p))

}

class VecReflectionContext(p: EntityPlayer) extends Context(p) {
  import cn.lambdalib.util.generic.RandUtils._
  import cn.academy.ability.api.AbilityAPIExt._
  import cn.lambdalib.util.mc.MCExtender._

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  def l_keyUp() = {
    terminate()
  }



}
