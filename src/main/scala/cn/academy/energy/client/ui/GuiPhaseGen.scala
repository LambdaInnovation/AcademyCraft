package cn.academy.energy.client.ui

import cn.academy.core.client.ui.ConfigPage.HistoElement
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.ContainerPhaseGen
import cn.academy.core.client.ui._
import cn.lambdalib.cgui.gui.HierarchyDebugger

object GuiPhaseGen {

  def apply(container: ContainerPhaseGen) = {
    val tile = container.tile
    val inventoryPage = InventoryPage("phasegen")
    val wirelessPage = WirelessPage.userPage(tile)

    val configPage = ConfigPage(
      Nil,
      Seq(ConfigPage.histoEnergy(() => tile.getEnergy, tile.bufferSize)))

    val ret = new ContainerUI(container,  inventoryPage, configPage, wirelessPage)

    ret
  }

}
