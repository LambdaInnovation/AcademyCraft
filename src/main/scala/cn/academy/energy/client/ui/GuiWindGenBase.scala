package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.wind.ContainerWindGenBase

import cn.academy.core.client.ui._

object GuiWindGenBase {

  def apply(container: ContainerWindGenBase) = {
    val tile = container.tile

    val invPage = InventoryPage("windbase")
    val configPage = ConfigPage(Nil, Nil)
    val wirelessPage = WirelessPage.userPage(tile)

    new ContainerUI(container, invPage, configPage, wirelessPage)
  }

}
