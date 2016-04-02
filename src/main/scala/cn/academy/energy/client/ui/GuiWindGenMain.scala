package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.wind.ContainerWindGenMain

import cn.academy.core.client.ui._

object GuiWindGenMain {

  def apply(container: ContainerWindGenMain) = {
    val tile = container.tile

    val invPage = InventoryPage("windmain")

    val ret = new ContainerUI(container, invPage)

    ret.infoPage.property("altitude", tile.yCoord)

    ret
  }

}
