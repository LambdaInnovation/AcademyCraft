package cn.academy.crafting.client.ui

import cn.academy.Resources
import cn.academy.block.container.ContainerMetalFormer
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.core.client.ui._
import cn.academy.block.tileentity.TileMetalFormer
import cn.lambdalib2.cgui.ScalaCGUI._
import cn.lambdalib2.cgui.Widget
import cn.lambdalib2.cgui.component.{DrawTexture, ProgressBar}
import cn.lambdalib2.cgui.event.{FrameEvent, LeftClickEvent}
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.NetworkS11nType
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib2.render.font.IFont.{FontAlign, FontOption}
import cn.lambdalib2.util.Colors
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.Side

object GuiMetalFormer {
  import MFNetDelegate._

  private lazy val template = CGUIDocument.read(Resources.getGui("rework/page_metalformer")).getWidget("main")

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

      def handleAlt(dir: Int) = () => send(MSG_ALTERNATE, tile, dir, Future.create2[TileMetalFormer.Mode](updateModeTexture))

      invWidget.child("btn_left").listens[LeftClickEvent](handleAlt(-1))
      invWidget.child("btn_right").listens[LeftClickEvent](handleAlt(1))

      {
        val option = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xaaffffff))
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

private object MFNetDelegate {

  @StateEventCallback
  def init(ev: FMLInitializationEvent) = {
    NetworkS11n.addDirectInstance(MFNetDelegate)
  }

  final val MSG_ALTERNATE = "alt"

  @Listener(channel=MSG_ALTERNATE, side=Array(Side.SERVER))
  def alternate(tile: TileMetalFormer, dir: Int, fut: Future[TileMetalFormer.Mode]) = {
    tile.cycleMode(dir)
    fut.sendResult(tile.mode)
  }

}