package cn.academy.crafting.client.ui

import cn.academy.block.container.ContainerImagFusor
import cn.academy.Resources
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.core.client.ui._
import cn.academy.util.LocalHelper
import cn.lambdalib2.cgui.ScalaCGUI._
import cn.lambdalib2.cgui.component.{ProgressBar, TextBox}
import cn.lambdalib2.cgui.event.FrameEvent
import cn.lambdalib2.cgui.loader.CGUIDocument

object GuiImagFusor {

  private lazy val template = CGUIDocument.read(Resources.getGui("rework/page_imagfusor")).getWidget("main")
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