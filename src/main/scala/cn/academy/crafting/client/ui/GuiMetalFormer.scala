package cn.academy.crafting.client.ui

import cn.academy.core.client.Resources
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.core.client.ui._
import cn.academy.crafting.block.{ContainerMetalFormer, TileMetalFormer}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.cgui.ScalaCGUI._
import cn.lambdalib.cgui.gui.Widget
import cn.lambdalib.cgui.gui.component.{DrawTexture, ProgressBar}
import cn.lambdalib.cgui.gui.event.{FrameEvent, LeftClickEvent}
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cn.lambdalib.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib.util.client.font.IFont.{FontAlign, FontOption}
import cn.lambdalib.util.helper.Color
import cpw.mods.fml.relauncher.Side

object GuiMetalFormer {
  import MFNetDelegate._

  private lazy val template = CGUIDocument.panicRead(Resources.getGui("rework/page_metalformer")).getWidget("main")

  def apply(container: ContainerMetalFormer) = {
    val tile = container.tile

    val invWidget = template.copy()

    {
      def updateModeTexture(mode: TileMetalFormer.Mode) = {
        invWidget.child("icon_mode").component[DrawTexture].texture = mode.texture
      }

      updateModeTexture(tile.mode)

      invWidget.getWidget("progress").listens((w: Widget, evt: FrameEvent) => {
        w.component[ProgressBar].progress = tile.getWorkProgress
      })

      def handleAlt(dir: Int) = () => send(MSG_ALTERNATE, tile, -1, Future.create[TileMetalFormer.Mode](updateModeTexture(_)))

      invWidget.child("btn_left").listens[LeftClickEvent](handleAlt(-1))
      invWidget.child("btn_right").listens[LeftClickEvent](handleAlt(1))

      {
        val option = new FontOption(10, FontAlign.CENTER, new Color(0xaaffffff))
        invWidget.child("icon_mode").listens((w: Widget, evt: FrameEvent) => if (evt.hovering) {
          TechUI.drawTextBox(tile.mode.toString, option, 6, -10)
        })
      }
    }

    val invPage = InventoryPage(invWidget)
    val wirelessPage = WirelessPage.userPage(tile)

    val ret = new ContainerUI(container, invPage, wirelessPage)

    ret.infoPage.histogram(TechUI.histEnergy(() => tile.getEnergy, tile.getMaxEnergy))

    ret
  }

  private def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(MFNetDelegate, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

}

@Registrant
@NetworkS11nType
private object MFNetDelegate {

  NetworkS11n.addDirectInstance(MFNetDelegate)

  final val MSG_ALTERNATE = "alt"

  @Listener(channel=MSG_ALTERNATE, side=Array(Side.SERVER))
  def alternate(tile: TileMetalFormer, dir: Int, fut: Future[TileMetalFormer.Mode]) = {
    tile.cycleMode(dir)
    fut.sendResult(tile.mode)
  }

}
