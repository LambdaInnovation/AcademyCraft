package cn.academy.energy.client.ui

import cn.academy.core.client.ui.ConfigPage.HistoElement
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.ContainerPhaseGen
import cn.academy.core.client.ui._
import cn.lambdalib.cgui.gui.HierarchyDebugger

object GuiPhaseGen2 {

  def apply(container: ContainerPhaseGen) = {
    val tile = container.tile
    val inventoryPage = InventoryPage("phasegen")
    val wirelessPage = WirelessPage(tile)

    val configPage = ConfigPage(
      Seq(ConfigPage.textPropertyUpdated(() => "Energy: " + tile.getEnergy + "/" + tile.bufferSize)),
      Seq(HistoElement("energy", ConfigPage.COLOR_ENERGY, () => tile.getEnergy / tile.bufferSize)))

    val ret = new ContainerUI(container,  inventoryPage, configPage, wirelessPage)
    ret.getGui.addWidget(new HierarchyDebugger)
    // ret.getGui.setDebug()

    ret
  }

}
