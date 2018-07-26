package cn.academy.misc.media

import cn.academy.terminal.{App, AppEnvironment, AppRegistry}
import cn.lambdalib2.annoreg.core.Registrant
import cn.lambdalib2.annoreg.mc.{RegInitCallback, RegPreInitCallback}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft

@Registrant
private object MediaAppInit {
  @RegPreInitCallback
  def init() = {
    AppRegistry.register(MediaApp)
  }
}

object MediaApp extends App("media_player") {

  override def createEnvironment(): AppEnvironment = new AppEnvironment {
    @SideOnly(Side.CLIENT)
    override def onStart(): Unit = {
      Minecraft.getMinecraft.displayGuiScreen(new MediaGui)
    }
  }

}
