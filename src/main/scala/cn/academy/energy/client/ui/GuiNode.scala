package cn.academy.energy.client.ui

import cn.academy.Resources
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.api.WirelessHelper
import cn.academy.block.block.BlockNode.NodeType
import cn.academy.block.container.ContainerNode
import cn.academy.block.tileentity.TileNode
import cn.academy.core.client.ui._
import cn.lambdalib2.cgui.ScalaCGUI._
import cn.lambdalib2.cgui.Widget
import cn.lambdalib2.cgui.event.FrameEvent
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.NetworkS11nType
import cn.lambdalib2.util.{GameTimer, HudUtils, RenderUtils}
import net.minecraftforge.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.lwjgl.opengl.GL11

object GuiNode {
  import NodeNetworkProxy._

  val STATE_LINKED = 0
  val STATE_UNLINKED = 1

  val ALL_FRAMES = 10

  val states = Array(State(0, 8, 800), State(8, 2, 3000))

  val animTexture = Resources.getTexture("guis/effect/effect_node")

  def apply(container: ContainerNode) = {
    val tile = container.tile
    val thePlayer = Minecraft.getMinecraft.player

    var state = STATE_UNLINKED
    def getState = states(state)

    val invPage = InventoryPage("node")

    {
      val animArea = new Widget().pos(42, 35.5f).size(186, 75).scale(0.5f)
      var stateContext = StateContext(getState, 0)

      animArea.listens[FrameEvent](() => {
        val state = getState
        if (stateContext.state != state) {
          stateContext = StateContext(state, 0)
        }

        stateContext.updateAndDraw(animArea.transform.width, animArea.transform.height)
      })

      invPage.window :+ animArea
    }

    val wirelessPage = WirelessPage.nodePage(tile)

    val ret = new ContainerUI(container, invPage, wirelessPage)

    {
      var load = 1

      send(MSG_INIT, tile, Future.create2((cap: Int) => load = cap))

      ret.infoPage
        .histogram(
          TechUI.histEnergy(() => tile.getEnergy, tile.getMaxEnergy),
          TechUI.histCapacity(() => load, tile.getCapacity))
          .seplineInfo()
          .property("range", tile.getRange)
          .property("owner", tile.getPlacerName)

      if (tile.getPlacerName == thePlayer.getName) {
        ret.infoPage
          .property("node_name", tile.getNodeName, newName => send(MSG_RENAME, thePlayer, tile, newName))
          .property("password", tile.getPassword, newPass => send(MSG_CHANGE_PASS, thePlayer, tile, newPass), password=true)
      } else {
        ret.infoPage
          .property("node_name", tile.getNodeName)
      }
    }

    { // Update node status listener
      var time = GameTimer.getTime - 2
      ret.main.listens[FrameEvent](() => {
        val dt = GameTimer.getTime - time
        if (dt > 2) {
          send(MSG_QUERY_LINK, tile, Future.create2((res: Boolean) => state = if (res) STATE_LINKED else STATE_UNLINKED))
          time = GameTimer.getTime
        }
      })
    }

    ret
  }

  private def send(channel: String, pars: Any*) =
    NetworkMessage.sendToServer(NodeNetworkProxy, channel, pars.map(_.asInstanceOf[AnyRef]): _*)

  case class StateContext(state: State, var frame: Int) {
    var lastChange: Double = GameTimer.getTime

    def updateAndDraw(w: Double, h: Double) = {
      val time = GameTimer.getTime
      val dt: Long = ((time - lastChange) * 1000).toLong

      if (dt >= state.frameTime) {
        lastChange = time
        frame = (frame + 1) % state.frames
      }

      val texFrame = state.begin + frame

      RenderUtils.loadTexture(animTexture)
      GL11.glColor4d(1, 1, 1, TechUI.breatheAlpha)
      HudUtils.rawRect(0, 0,
        0, texFrame.toDouble / ALL_FRAMES,
        w, h,
        1, 1.0 / ALL_FRAMES)
    }
  }
  case class State(begin: Int, frames: Int, frameTime: Long)

}

@NetworkS11nType
object NodeNetworkProxy {
  final val MSG_RENAME = "rename"
  final val MSG_CHANGE_PASS = "repass"
  final val MSG_INIT   = "init"
  final val MSG_QUERY_LINK = "query_link"

  @StateEventCallback
  def __init(ev: FMLInitializationEvent) = {
    NetworkS11n.addDirectInstance(NodeNetworkProxy)
  }

  @Listener(channel=MSG_RENAME, side=Array(Side.SERVER))
  def rename(player: EntityPlayer, node: TileNode, name: String) = {
    if (player.getName == node.getPlacerName) {
      node.setNodeName(name)
    }
  }

  @Listener(channel=MSG_CHANGE_PASS, side=Array(Side.SERVER))
  def changePassword(player: EntityPlayer, node: TileNode, name: String) = {
    if (player.getName == node.getPlacerName) {
      node.setPassword(name)
    }
  }

  @Listener(channel=MSG_QUERY_LINK, side=Array(Side.SERVER))
  def queryIsLinked(node: TileNode, future: Future[Boolean]) = {
    future.sendResult(WirelessHelper.isNodeLinked(node))
  }

  @Listener(channel=MSG_INIT, side=Array(Side.SERVER))
  def init(node: TileNode, future: Future[Int]) = {
    val conn = WirelessHelper.getNodeConn(node)
    future.sendResult(conn.getLoad)
  }

}