package cn.academy.crafting.client.ui

import cn.academy.core.{LocalHelper, Resources}
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.core.client.ui._
import cn.academy.crafting.block.ContainerImagFusor
import cn.lambdalib.cgui.ScalaCGUI._
import cn.lambdalib.cgui.gui.component.{ProgressBar, TextBox}
import cn.lambdalib.cgui.gui.event.FrameEvent
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.util.helper.Color

object GuiImagFusor {

  private lazy val template = CGUIDocument.panicRead(Resources.getGui("rework/page_imagfusor")).getWidget("main")
  private val local = LocalHelper.at("ac.imag_fusor")

  def apply(container: ContainerImagFusor) = {
    val tile = container.tile

    val invPage = InventoryPage(template.copy())

    val ret = new ContainerUI(container, invPage, WirelessPage.userPage(tile))

    { // Work progress display
      val progWidget = invPage.window.getWidget("progress")
      val bar = progWidget.component[ProgressBar]

      progWidget.listens[FrameEvent](() => {
        bar.progress = tile.getWorkProgress
      })
    }

    { // Imag requirement display
      val reqWidget = invPage.window.getWidget("text_imagneeded")
      val text = reqWidget.component[TextBox]

      text.content = "IDLE"

      reqWidget.listens[FrameEvent](() => {
        val recipe = tile.getCurrentRecipe
        text.content = if (recipe == null) "IDLE" else String.valueOf(recipe.consumeLiquid)
      })
    }

    ret.infoPage.histogram(
      TechUI.histEnergy(() => tile.getEnergyForDisplay, tile.getMaxEnergy),
      TechUI.histPhaseLiquid(() => tile.getLiquidAmount, tile.getTankSize))

    ret
  }

}
