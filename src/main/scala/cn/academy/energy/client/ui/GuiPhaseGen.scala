package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.ContainerPhaseGen
import cn.academy.core.client.ui._
import cn.lambdalib.util.helper.Color

object GuiPhaseGen {

  def apply(container: ContainerPhaseGen) = {
    val tile = container.tile
    val inventoryPage = InventoryPage("phasegen")
    val wirelessPage = WirelessPage.userPage(tile)

    val ret = new ContainerUI(container,  inventoryPage, wirelessPage)
    ret.infoPage.histogram(
      TechUI.histEnergy(() => tile.getEnergy, tile.bufferSize),
      TechUI.HistElement("IF", new Color(0xffb983fb),
        () => tile.getLiquidAmount.toDouble / tile.getTankSize, () => "%d mB".format(tile.getLiquidAmount)))

    ret
  }

}
