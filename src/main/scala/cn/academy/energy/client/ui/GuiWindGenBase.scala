package cn.academy.energy.client.ui

import cn.academy.Resources
import cn.academy.block.container.ContainerWindGenBase
import cn.academy.block.tileentity.TileWindGenBase
import cn.academy.core.client.ui.TechUI.{ContainerUI, HistElement}
import cn.academy.core.client.ui._
import cn.lambdalib2.cgui.component.{DrawTexture, TextBox}
import cn.lambdalib2.cgui.event.FrameEvent
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.util.Colors

object GuiWindGenBase {
  import cn.lambdalib2.cgui.ScalaCGUI._

  private lazy val template = CGUIDocument.read(Resources.getGui("rework/page_windbase")).getWidget("main")

  def apply(container: ContainerWindGenBase) = {
    val tile = container.tile
    val main = template.copy()

    {
      import TileWindGenBase.Completeness
      val ui = main.child("ui_block")
      val texMain = ui.child("icon_main").component[DrawTexture]
      val texMiddle = ui.child("icon_middle").component[DrawTexture]
      val texBase = ui.child("icon_base").component[DrawTexture]

      val (a0, a1, a2) = (0.2f, 0.6f, 1.0f)

      ui.listens[FrameEvent](() => {
        val (amain, amiddle, abase) = tile.getCompleteness match {
          case Completeness.COMPLETE_NOT_WORKING => (a1, a2, a2)
          case Completeness.COMPLETE => (a2, a2, a2)
          case Completeness.NO_TOP => (a0, a2, a2)
          case Completeness.BASE_ONLY => (a0, a0, a2)
        }
        texBase.color.setAlpha(Colors.f2i(abase))
        texMain.color.setAlpha(Colors.f2i(amain))
        texMiddle.color.setAlpha(Colors.f2i(amiddle))
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
      .property("altitude", tile.getPos.getY)

    ret
  }

}