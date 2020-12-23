package cn.academy.energy.client.ui

import cn.academy.block.container.ContainerMatrix
import cn.academy.block.tileentity.TileMatrix
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.api.WirelessHelper
import cn.academy.event.energy.{ChangePassEvent, CreateNetworkEvent}
import cn.academy.core.client.ui._
import cn.lambdalib2.cgui.Widget
import cn.lambdalib2.cgui.component.TextBox
import cn.lambdalib2.s11n.{SerializeNullable, SerializeStrategy}
import cn.lambdalib2.s11n.SerializeStrategy.ExposeStrategy
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.NetworkS11nType
import net.minecraftforge.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import cn.lambdalib2.cgui.ScalaCGUI._
import cn.lambdalib2.registry.StateEventCallback
import net.minecraftforge.fml.common.event.FMLInitializationEvent

object GuiMatrix2 {
  import MatrixNetProxy._

  def apply(container: ContainerMatrix) = {
    val tile = container.tile
    val thePlayer = Minecraft.getMinecraft.player
    val isPlacer = tile.getPlacerName == thePlayer.getName

    val invPage = InventoryPage("matrix")

    val ret = new ContainerUI(container, invPage)

    {
      def rebuildInfo(data: InitData): Unit = {
        ret.infoPage.reset()

        ret.infoPage.histogram(
          TechUI.histCapacity(() => data.load, tile.getCapacity)
        )

        ret.infoPage.seplineInfo()
          .property("owner", tile.getPlacerName)
          .property("range", "%.0f".format(tile.getRange))
          .property("bandwidth", tile.getBandwidth + " IF/T")

        if (data.init) {
          ret.infoPage.sepline("wireless_info")
          if (isPlacer) {
            ret.infoPage
              .property("ssid", data.ssid, editCallback = newSSID => {
              send(MSG_CHANGE_SSID, tile, thePlayer, newSSID)
              })
              .sepline("change_pass")
              .property("password", data.pass, password=true, editCallback = newPass => {
                send(MSG_CHANGE_PASSWORD, tile, thePlayer, newPass)
              })
          } else {
            ret.infoPage
              .property("ssid", data.ssid)
              .property("password", data.pass, password=true)
          }
        } else {
          val ssidCell = Array[TextBox](null)
          val passwordCell = Array[TextBox](null)

          if (isPlacer) {
            ret.infoPage
              .sepline("wireless_init")
              .property("ssid", "", _ => {}, contentCell=ssidCell, colorChange=false)
              .property("password", "", _ => {}, contentCell=passwordCell, password=true, colorChange=false)
              .blank(1)
              .button("INIT", () => {
                val (ssidBox, passBox) = (ssidCell(0), passwordCell(0))
                send(MSG_INIT, tile, ssidBox.content, passBox.content, Future.create2((_: Boolean) =>
                  send(MSG_GATHER_INFO, tile, Future.create2((inf: InitData) => rebuildInfo(inf)))
                ))
              })
          } else {
            ret.infoPage.sepline("wireless_noinit")
          }
        }
      }

      send(MSG_GATHER_INFO, tile, Future.create2((inf: InitData) => rebuildInfo(inf)))
    }

    ret
  }

  private def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(MatrixNetProxy, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

}

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

@NetworkS11nType
private object MatrixNetProxy {

  @StateEventCallback
  def __init(ev: FMLInitializationEvent) = {
    NetworkS11n.addDirectInstance(MatrixNetProxy)
  }

  final val MSG_GATHER_INFO = "gather"
  final val MSG_INIT = "init"
  final val MSG_CHANGE_PASSWORD = "pass"
  final val MSG_CHANGE_SSID = "ssid"

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
    if (matrix.getPlacerName == player.getName) {
      MinecraftForge.EVENT_BUS.post(new ChangePassEvent(matrix, pwd))
    }
  }

  @Listener(channel=MSG_CHANGE_SSID, side=Array(Side.SERVER))
  def changeSSID(matrix: TileMatrix, player: EntityPlayer, newSSID: String) = {
    if (matrix.getPlacerName == player.getName) {
      Option(WirelessHelper.getWirelessNet(matrix)) match {
        case Some(net) =>
          net.setSSID(newSSID)
        case _ =>
      }
    }
  }

}