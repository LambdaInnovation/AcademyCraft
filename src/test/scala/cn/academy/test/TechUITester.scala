package cn.academy.test

import cn.academy.core.client.ui.TechUI
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.CGuiScreen
import cn.lambdalib.util.helper.{GameTimer, Color}
import cn.lambdalib.util.key.{KeyHandler, KeyManager}
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard

@Registrant
object TechUITester {

  @RegInitCallback
  def init() = {
    KeyManager.dynamic.addKeyHandler("testTechUI", Keyboard.KEY_G, new KeyHandler {

      override def onKeyDown() = {
        val gui = new CGuiScreen()

        val pages = TechUI.create(TechUI.createConfigPage(
          Seq(TechUI.textProperty("23333333"),
            TechUI.textProperty("hehehehhe"),
            TechUI.textProperty("Neptune is moe")),
          Seq(TechUI.HistoElement("www", new Color(0xffff98ed), () => 1.0f),
            TechUI.HistoElement("www2", new Color(0xffff8989), () => 0.5f))),
          TechUI.createWirelessPage())
        gui.getGui.addWidget(pages)

        getMC.displayGuiScreen(gui)
      }

    })
  }

}
