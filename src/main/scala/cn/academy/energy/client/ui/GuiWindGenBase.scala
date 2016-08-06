package cn.academy.energy.client.ui

import cn.academy.core.Resources
import cn.academy.core.client.ui.TechUI.{ContainerUI, HistElement}
import cn.academy.energy.block.wind.{ContainerWindGenBase, TileWindGenBase}
import cn.academy.core.client.ui._
import cn.lambdalib.cgui.gui.component.{DrawTexture, TextBox}
import cn.lambdalib.cgui.gui.event.FrameEvent
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.util.helper.Color

object GuiWindGenBase {
  import cn.lambdalib.cgui.ScalaCGUI._

  private lazy val template = CGUIDocument.panicRead(Resources.getGui("rework/page_windbase")).getWidget("main")

  def apply(container: ContainerWindGenBase) = {
    val tile = container.tile
    val main = template.copy()

    {
      import TileWindGenBase.Completeness
      val ui = main.child("ui_block")
      val texMain = ui.child("icon_main").component[DrawTexture]
      val texMiddle = ui.child("icon_middle").component[DrawTexture]
      val texBase = ui.child("icon_base").component[DrawTexture]

      val (a0, a1, a2) = (0.2, 0.6, 1.0)

      ui.listens[FrameEvent](() => {
        val (amain, amiddle, abase) = tile.getCompleteness match {
          case Completeness.COMPLETE_NOT_WORKING => (a1, a2, a2)
          case Completeness.COMPLETE => (a2, a2, a2)
          case Completeness.NO_TOP => (a0, a2, a2)
          case Completeness.BASE_ONLY => (a0, a0, a2)
        }
        texBase.color.a = abase
        texMain.color.a = amain
        texMiddle.color.a = amiddle
      })
    }


    val invPage = InventoryPage(main)
    val wirelessPage = WirelessPage.userPage(tile)

    val ret = new ContainerUI(container, invPage, wirelessPage)

    ret.infoPage
      .histogram(
        TechUI.histBuffer(() => tile.getEnergy, tile.bufferSize)
      )
      .seplineInfo()
      .property("altitude", tile.yCoord)

    ret
  }

}
