package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.api.WirelessHelper
import cn.academy.energy.api.event.wen.{ChangePassEvent, CreateNetworkEvent}
import cn.academy.energy.block.{TileMatrix, ContainerMatrix}

import cn.academy.core.client.ui._
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.Widget
import cn.lambdalib.cgui.gui.component.TextBox
import cn.lambdalib.s11n.{SerializeNullable, SerializeStrategy}
import cn.lambdalib.s11n.SerializeStrategy.ExposeStrategy
import cn.lambdalib.s11n.network.{NetworkS11n, NetworkMessage, Future}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cn.lambdalib.util.helper.Color
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import cn.lambdalib.cgui.ScalaCGUI._

object GuiMatrix2 {
  import MatrixNetProxy._

  def apply(container: ContainerMatrix) = {
    val tile = container.tile
    val thePlayer = Minecraft.getMinecraft.thePlayer

    val invPage = InventoryPage("matrix")

    val ret = new ContainerUI(container, invPage)

    {
      def rebuildInfo(data: InitData): Unit = {
        ret.infoPage.reset()

        val loadPct = if (tile.getCapacity == 0) 0 else data.load.toDouble / tile.getCapacity
        val loadStr = "%d/%d".format(data.load, tile.getCapacity)

        ret.infoPage.histogram(
          TechUI.HistElement("CAPACITY", new Color(0x5aa0e2), () => loadPct, () => loadStr)
        )

        ret.infoPage.sepline("INFO")
          .property("RANGE", tile.getRange)
          .property("BANDWIDTH", tile.getBandwidth + " IF/T")
          .property("OWNER", tile.getPlacerName)

        if (data.init) {
          ret.infoPage
            .sepline("WIRELESS INFO")
            .property("SSID", data.ssid)
            .sepline("CHANGE PASSWORD")
            .property("PASSWORD", data.pass, password=true, editCallback = newPass => {
              send(MSG_CHANGE_PASSWORD, tile, thePlayer, newPass)
            })
        } else {
          val ssidCell = Array[TextBox](null)
          val passwordCell = Array[TextBox](null)

          ret.infoPage
            .sepline("WIRELESS INIT")
            .property("SSID", "", _ => {}, contentCell=ssidCell, colorChange=false)
            .property("PASSWORD", "", _ => {}, contentCell=passwordCell, password=true, colorChange=false)
            .blank(1)
            .button("INIT", () => {
              val (ssidBox, passBox) = (ssidCell(0), passwordCell(0))
              send(MSG_INIT, tile, ssidBox.content, passBox.content, Future.create((_: Boolean) =>
                send(MSG_GATHER_INFO, tile, Future.create((inf: InitData) => rebuildInfo(inf)))
              ))
            })
        }
      }

      send(MSG_GATHER_INFO, tile, Future.create((inf: InitData) => rebuildInfo(inf)))
    }

    ret
  }

  private def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(MatrixNetProxy, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

}

@Registrant
@NetworkS11nType
@SerializeStrategy(strategy=ExposeStrategy.ALL)
private class InitData {
  @SerializeNullable
  var ssid: String = null
  @SerializeNullable
  var pass: String = null
  var load: Int = 0

  def init = ssid != null
}

@Registrant
@NetworkS11nType
private object MatrixNetProxy {

  @RegInitCallback
  def __init() = {
    NetworkS11n.addDirectInstance(MatrixNetProxy)
  }

  final val MSG_GATHER_INFO = "gather"
  final val MSG_INIT = "init"
  final val MSG_CHANGE_PASSWORD = "pass"

  @Listener(channel=MSG_GATHER_INFO, side=Array(Side.SERVER))
  def gatherInfo(matrix: TileMatrix, future: Future[InitData]) = {
    val optNetwork = Option(WirelessHelper.getWirelessNet(matrix))
    val result = new InitData
    optNetwork match {
      case Some(net) =>
        result.ssid = net.getSSID
        result.pass = net.getPassword
        result.load = net.getLoad
      case _ =>
    }

    future.sendResult(result)
  }

  @Listener(channel=MSG_INIT, side=Array(Side.SERVER))
  def init(matrix: TileMatrix, ssid: String, pwd: String, fut: Future[Boolean]) = {
    MinecraftForge.EVENT_BUS.post(new CreateNetworkEvent(matrix, ssid, pwd))

    fut.sendResult(true)
  }

  @Listener(channel=MSG_CHANGE_PASSWORD, side=Array(Side.SERVER))
  def changePassword(matrix: TileMatrix, player: EntityPlayer, pwd: String) = {
    if (matrix.getPlacerName == player.getCommandSenderName) {
      MinecraftForge.EVENT_BUS.post(new ChangePassEvent(matrix, pwd))
    }
  }

}
