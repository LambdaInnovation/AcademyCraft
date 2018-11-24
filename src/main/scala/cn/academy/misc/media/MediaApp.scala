package cn.academy.misc.media

import cn.academy.terminal.{App, AppEnvironment, AppRegistry}
import cn.lambdalib2.registry.StateEventCallback
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent

private object MediaAppInit {
  @StateEventCallback
  def init(ev: FMLPreInitializationEvent) = {
    AppRegistry.register(MediaApp)
  }
}

object MediaApp extends App("media_player") {

  @SideOnly(Side.CLIENT)
  override def createEnvironment(): AppEnvironment = new AppEnvironment {
    override def onStart(): Unit = {
      Minecraft.getMinecraft.displayGuiScreen(new MediaGui)
    }
  }

}