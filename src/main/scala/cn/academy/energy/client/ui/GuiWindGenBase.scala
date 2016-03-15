package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.{HistElement, ContainerUI}
import cn.academy.energy.block.wind.ContainerWindGenBase

import cn.academy.core.client.ui._
import cn.lambdalib.util.helper.Color

object GuiWindGenBase {

  def apply(container: ContainerWindGenBase) = {
    val tile = container.tile

    val invPage = InventoryPage("windbase")
    val wirelessPage = WirelessPage.userPage(tile)

    val ret = new ContainerUI(container, invPage, wirelessPage)

    ret.infoPage
      .histogram(
        TechUI.histBuffer(() => tile.getEnergy, tile.bufferSize)
      )
      .sepline("INFO")
      .property("ALTITUDE", tile.yCoord)

    ret
  }

}
