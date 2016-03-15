package cn.academy.energy.client.ui

import cn.academy.core.client.Resources
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.energy.block.BlockNode.NodeType
import cn.academy.energy.block.{TileNode, ContainerNode}

import cn.academy.core.client.ui._
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.cgui.ScalaCGUI._
import cn.lambdalib.cgui.gui.Widget
import cn.lambdalib.cgui.gui.event.FrameEvent
import cn.lambdalib.s11n.network.{NetworkS11n, NetworkMessage}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cn.lambdalib.util.client.{RenderUtils, HudUtils}
import cn.lambdalib.util.helper.{Color, GameTimer}
import cpw.mods.fml.relauncher.Side
import org.lwjgl.opengl.GL11

object GuiNode2 {
  import NodeNetworkProxy._

  val STATE_LINKED = 0
  val STATE_UNLINKED = 1

  val ALL_FRAMES = 10

  val states = Array(State(0, 8, 800), State(8, 2, 3000))

  val animTexture = Resources.getTexture("guis/effect/effect_node")

  def apply(container: ContainerNode) = {
    val tile = container.node

    def getState = states(STATE_LINKED)

    val invPage = InventoryPage("node")

    {
      val animArea = new Widget().pos(42, 35.5).size(186, 75).scale(0.5)
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
      var currentCapacity = 1 // TODO: Send packet at init to retrieve that

      ret.infoPage
        .histogram(
          TechUI.histEnergy(() => tile.getEnergy, tile.getMaxEnergy),
          TechUI.HistElement("CAPACITY", new Color(0xffff6c00),
            () => currentCapacity / tile.getCapacity, () => "%d/%d".format(currentCapacity, tile.getCapacity)),
          TechUI.HistElement("RANGE", new Color(0xff7680de),
            () => tile.getRange / NodeType.ADVANCED.range, () => "%.0f".format(tile.getRange))
          )
          .property("NAME", tile.getNodeName, newName => send(MSG_RENAME, newName))
    }

    ret
  }

  private def send(channel: String, pars: Any*) =
    NetworkMessage.sendToServer(NodeNetworkProxy, channel, pars.map(_.asInstanceOf[AnyRef]): _*)

  case class StateContext(val state: State, var frame: Int) {
    var lastChange: Long = GameTimer.getTime

    def updateAndDraw(w: Double, h: Double) = {
      val time = GameTimer.getTime
      val dt = time - lastChange

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
  case class State(val begin: Int, val frames: Int, val frameTime: Long)

}

@Registrant
@NetworkS11nType
object NodeNetworkProxy {
  final val MSG_RENAME = "rename"

  NetworkS11n.addDirectInstance(NodeNetworkProxy)

  @Listener(channel=MSG_RENAME, side=Array(Side.SERVER))
  def rename(node: TileNode, name: String) = {
    node.setNodeName(name)
  }

}
