package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.wind.ContainerWindGenMain

import cn.academy.core.client.ui._

object GuiWindGenMain2 {

  def apply(container: ContainerWindGenMain) = {
    val tile = container.tile

    val invPage = InventoryPage("windmain")
    val configPage = ConfigPage(Nil, Nil)

    new ContainerUI(container, invPage, configPage)
  }

}
