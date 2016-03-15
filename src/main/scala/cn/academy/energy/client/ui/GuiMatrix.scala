package cn.academy.energy.client.ui

import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.api.WirelessHelper
import cn.academy.energy.api.event.wen.CreateNetworkEvent
import cn.academy.energy.block.{TileMatrix, ContainerMatrix}

import cn.academy.core.client.ui._
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.cgui.gui.Widget
import cn.lambdalib.cgui.gui.component.TextBox
import cn.lambdalib.s11n.network.{NetworkS11n, NetworkMessage, Future}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cpw.mods.fml.relauncher.Side
import net.minecraftforge.common.MinecraftForge
import cn.lambdalib.cgui.ScalaCGUI._

object GuiMatrix2 {
  import MatrixNetProxy._

  def apply(container: ContainerMatrix) = {
    val tile = container.tile
    val invPage = InventoryPage("matrix")

    val ret = new ContainerUI(container, invPage)

    ret
  }

  private def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(MatrixNetProxy, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

}

@Registrant
@NetworkS11nType
private object MatrixNetProxy {

  NetworkS11n.addDirectInstance(MatrixNetProxy)
  NetworkS11n.register(classOf[Array[String]])

  final val MSG_GATHER_INFO = "gather"
  final val MSG_INIT = "init"

  @Listener(channel=MSG_GATHER_INFO, side=Array(Side.SERVER))
  def gatherInfo(matrix: TileMatrix, future: Future[Array[String]]) = {
    val optNetwork = Option(WirelessHelper.getWirelessNet(matrix))
    val result: Array[String] = optNetwork match {
      case Some(network) => Array(network.getSSID, network.getPassword)
      case None => Array(null, null)
    }

    future.sendResult(result)
  }

  @Listener(channel=MSG_INIT, side=Array(Side.SERVER))
  def init(matrix: TileMatrix, ssid: String, pwd: String, fut: Future[Boolean]) = {
    println("IB")

    MinecraftForge.EVENT_BUS.post(new CreateNetworkEvent(matrix, ssid, pwd))

    fut.sendResult(true)
  }

}
