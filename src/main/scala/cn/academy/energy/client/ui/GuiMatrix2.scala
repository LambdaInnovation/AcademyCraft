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
  import MatrixNetDelegate._

  def apply(container: ContainerMatrix) = {
    val tile = container.tile

    val invPage = InventoryPage("matrix")

    val configPage = ConfigPage(Nil, Nil)

    def refreshInfo(): Unit = {
      println("A")

      send(MSG_GATHER_INFO, tile, Future.create((arr: Array[String]) => {
        println("C")

        val (ssid, pwd) = (arr(0), arr(1))

        def content(kv: Widget) = kv.child("value").component[TextBox].content

        val elements =
          if (ssid == null) {
            val ssid = ConfigPage.textBoxProperty("SSID", "", altColor = false)
            val pass = ConfigPage.textBoxProperty("PASS", "", altColor = false, password = true)
            val passConfirm = ConfigPage.textBoxProperty("CONFIRM", "", altColor = false, password = true)

            List(
              ssid,
              pass,
              passConfirm,
              ConfigPage.flowProperty(10, ConfigPage.button("INIT", () => {
                if (content(pass) == content(passConfirm)) {
                  send(MSG_INIT, tile, content(ssid), content(pass),
                    Future.create((_: Boolean) => refreshInfo()))
                }
              })))
          } else {
            List(
              ConfigPage.textBoxProperty("SSID", ssid, canEdit = false)
            )
          }

        ConfigPage.updateElements(configPage.window, elements)
      }))
    }

    refreshInfo()

    val ret = new ContainerUI(container, invPage, configPage)

    ret
  }

  private def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(MatrixNetDelegate, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

}

@Registrant
@NetworkS11nType
private object MatrixNetDelegate {

  NetworkS11n.addDirectInstance(MatrixNetDelegate)
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
