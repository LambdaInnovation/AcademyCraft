package cn.academy.core.client.ui

import cn.academy.core.client.Resources
import cn.academy.core.client.ui.TechUI.Page
import cn.academy.energy.api.WirelessHelper
import cn.academy.energy.api.block.{IWirelessMatrix, IWirelessNode, IWirelessUser, IWirelessTile}
import cn.academy.energy.api.event.node.{UnlinkUserEvent, LinkUserEvent}
import cn.academy.energy.api.event.wen.{UnlinkNodeEvent, LinkNodeEvent}
import cn.academy.energy.internal.{NodeConn, WirelessNet}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.component.TextBox.{ChangeContentEvent, ConfirmInputEvent}
import cn.lambdalib.cgui.gui.{CGuiScreenContainer, Widget}
import cn.lambdalib.cgui.gui.component.ProgressBar.Direction
import cn.lambdalib.cgui.gui.component._
import cn.lambdalib.cgui.gui.component.Transform.{HeightAlign, WidthAlign}
import cn.lambdalib.cgui.gui.event.{GainFocusEvent, LostFocusEvent, FrameEvent, LeftClickEvent}
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.s11n.SerializeStrategy.ExposeStrategy
import cn.lambdalib.s11n.{SerializeIncluded, SerializeNullable, SerializeStrategy}
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cn.lambdalib.s11n.network.{NetworkS11n, Future, NetworkMessage}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.client.font.IFont.FontOption
import cn.lambdalib.util.helper.{GameTimer, Color}
import cpw.mods.fml.relauncher.Side
import net.minecraft.inventory.Container
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{StatCollector, ResourceLocation}
import cn.lambdalib.cgui.ScalaCGUI._
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import scala.collection.JavaConversions._
import scala.reflect.ClassTag

private object Generic_ {
  def readxml(loc: String) = CGUIDocument.panicRead(new ResourceLocation(s"academy:guis/rework/$loc.xml"))
}

import Generic_._

object TechUI {

  private val pageButtonTemplate = readxml("pageselect").getWidget("main")

  case class Page(id: String, window: Widget)

  /**
    * Creates a tech UI with specified pages.
    *
    * @param pages The pages of this UI. Must not be empty.
    */
  def apply(pages: Page*) = new TechUIWidget(pages: _*)

  def breathe(widget: Widget) = {
    val tex = widget.getComponent(classOf[DrawTexture])
    widget.listens[FrameEvent](() => {
      tex.color.a = breatheAlpha
    })
  }

  /**
    * A global alpha value generator for producing uniform breathing effect.
    */
  def breatheAlpha = {
    val time = GameTimer.getTime
    val sin = (1 + math.sin(time / 500.0)) * 0.5

    0.5 + sin * 0.35
  }

  class TechUIWidget(pages: Page*) extends Widget {

    size(172, 187)
    centered()

    private var currentPage_ = pages.head

    pages.zipWithIndex.foreach { case (page, idx) =>
      val button = pageButtonTemplate.copy()
      button.walign(WidthAlign.LEFT).halign(HeightAlign.TOP)

      val buttonTex = button.getComponent(classOf[DrawTexture])
      buttonTex.setTex(Resources.getTexture("guis/icons/icon_" + page.id))
      button.scale(0.7)
      button.pos(-20, idx * 22)
      button.listens[LeftClickEvent](() => {
        pages.foreach(_.window.transform.doesDraw = false)
        page.window.transform.doesDraw = true
        currentPage_ = page
      })
      button.listens((evt: FrameEvent) => {
        val a1 = if (evt.hovering || currentPage_ == page) 1.0 else 0.8
        val a2 = if (currentPage_ == page) 1.0 else 0.8
        buttonTex.color.a = a1
        buttonTex.color.r = a2
        buttonTex.color.g = a2
        buttonTex.color.b = a2
      })

      page.window.transform.doesDraw = false

      this :+ button
      this :+ page.window
    }

    pages.head.window.transform.doesDraw = true

    def currentPage = currentPage_
  }

  class ContainerUI(container: Container, pages: Page*) extends CGuiScreenContainer(container) {
    val main = TechUI(pages: _*)

    gui.addWidget(main)

    def shouldDisplayInventory(page: Page): Boolean = page.id == "inv"

    override def isSlotActive = shouldDisplayInventory(main.currentPage)
  }

}

object ConfigPage {
  val COLOR_ENERGY = new Color(0xff25c4ff)
  val ELEM_W = 142
  val ELEM_H = 12

  private val configPageTemplate = readxml("page_config").getWidget("main")

  private val histoDescOption = new FontOption(9, 0xffd2d2d2)

  case class HistoElement(id: String, color: Color, progressProvider: () => Double, descProvider: () => String)

  def histoEnergy(energyProvider: () => Double, maxEnergy: Double) = {
    HistoElement("energy", COLOR_ENERGY, () => energyProvider() / maxEnergy,
      () => "Energy: %.0f/%.0f".format(energyProvider(), maxEnergy))
  }

  def textProperty(content: String, color: Color = Color.white()) = {
    val ret = pContainer()
    ret :+ new TextBox(new FontOption(10, color)).setContent(content)
    ret
  }

  def textPropertyUpdated(contentProvider: () => String, color: Color = Color.white()) = {
    val ret = pContainer()
    val textBox = new TextBox(new FontOption(10, color)).setContent(contentProvider())
    ret :+ textBox
    ret.listens[FrameEvent](() => textBox.setContent(contentProvider()))

    ret
  }

  def doubleProperty(name: String, initial: Double, editCallback: Double => Boolean) = {
    textBoxProperty(name, String.valueOf(initial), (content: String) => {
      try {
        val parsed = content.toDouble
        editCallback(parsed)
      } catch {
        case _: NumberFormatException => false
      }
    })
  }

  def textBoxProperty(name: String, initial: String, editCallback: String => Boolean) = {
    val NORMAL_COLOR   = new Color(0xaa797979)
    val MODIFIED_COLOR = new Color(0xaaa07d47)
    val ERROR_COLOR    = new Color(0xaae36a52)

    val editArea = new Widget().size(60, 12)

    val back = new DrawTexture().setColor(NORMAL_COLOR).setTex(null)
    val textBox = new TextBox().setContent(initial).allowEdit()

    editArea :+ back
    editArea :+ textBox

    editArea.listens[ChangeContentEvent](() => {
      back.setColor(MODIFIED_COLOR)
    })
    editArea.listens[ConfirmInputEvent](() => {
      val result = editCallback(textBox.content)
      back.setColor(if (result) NORMAL_COLOR else ERROR_COLOR)
    })

    kvpair(name, editArea)
  }

  private def pContainer() = new Widget().size(ELEM_W, ELEM_H)

  def kvpair(name: String, valueWidget: Widget) = {
    val container = pContainer()

    val nameArea = new Widget(50, 12)
    nameArea :+ new TextBox().setContent(name)

    valueWidget.pos(50, 0)// .size(ELEM_W - 50, ELEM_H)

    container :+ ("name", nameArea)
    container :+ ("value", valueWidget)

    container
  }

  def apply(properties: Seq[Widget], histo: Seq[HistoElement]) = {
    val widget = configPageTemplate.copy()

    TechUI.breathe(widget.getWidget("ui_info"))

    {
      val elist = new ElementList
      elist.spacing = 3.0
      properties.foreach(elist.addWidget)

      val panelConfig = widget.getWidget("panel_config")

      val area = panelConfig.getWidget("zone_elementlist")
      area :+ elist

      panelConfig.getWidget("btn_arrow_up").listens[LeftClickEvent](() => elist.progressLast())
      panelConfig.getWidget("btn_arrow_down").listens[LeftClickEvent](() => elist.progressNext())
    }

    {
      val panelDiagram = widget.getWidget("panel_diagram")
      val histZone = panelDiagram.getWidget("zone_histogram")
      val elemList = panelDiagram.getWidget("zone_elementlist")

      histo.zipWithIndex.foreach { case (elem, idx) =>
        val barX = 10 + idx * 15
        val bar = new Widget().halign(HeightAlign.BOTTOM).pos(barX, 0).size(10, 60)

        val progress = new ProgressBar().setDirection(Direction.UP).setFluctRegion(0)
        progress.color.from(elem.color)
        bar :+ progress
        bar.listens((evt: FrameEvent) => {
          progress.progress = elem.progressProvider()

          if (evt.hovering && (1 - evt.my / bar.transform.height) <= progress.progress) {
            val font = Resources.font()
            font.draw(elem.descProvider(), evt.mx + 5, evt.my - 5, histoDescOption)
          }
        })

        histZone :+ bar

        val disp = elemList.getWidget("element").copy()
        disp.transform.y += 10 + idx * 15
        disp.transform.doesDraw = true

        disp.getWidget("element_mark").getComponent(classOf[DrawTexture]).color.from(elem.color)
        disp.getWidget("element_name").getComponent(classOf[TextBox])
          .setContent(StatCollector.translateToLocal("ac.gui.histogram." + elem.id))

        elemList :+ disp
      }
    }

    Page("config_2", widget)
  }
}

@Registrant
@NetworkS11nType
private class UserResult {
  @SerializeIncluded
  @SerializeNullable
  var linked: NodeData = null
  @SerializeIncluded
  var avail: Array[NodeData] = null
}

@Registrant
@NetworkS11nType
private class NodeResult {
  @SerializeIncluded
  @SerializeNullable
  var linked: MatrixData = null
  @SerializeIncluded
  var avail: Array[MatrixData] = null
}

@Registrant
@NetworkS11nType
@SerializeStrategy(strategy=ExposeStrategy.ALL)
private class MatrixData {
  var x: Int = 0
  var y: Int = 0
  var z: Int = 0
  var ssid: String = null

  def tile(world: World) = world.getTileEntity(x, y, z) match {
    case tile: IWirelessMatrix => Some(tile)
    case _ => None
  }
}

@Registrant
@NetworkS11nType
@SerializeStrategy(strategy=ExposeStrategy.ALL)
private class NodeData {
  var x: Int = 0
  var y: Int = 0
  var z: Int = 0

  def tile(world: World) = world.getTileEntity(x, y, z) match {
    case tile: IWirelessNode => Some(tile)
    case _ => None
  }
}

object WirelessPage {
  type TileUser = TileEntity with IWirelessUser
  type TileNode = TileEntity with IWirelessNode
  type TileBase = TileEntity with IWirelessTile
  type TileMatrix = TileEntity with IWirelessMatrix

  private val wirelessPageTemplate = readxml("page_wireless").getWidget("main")

  private val connectedIcon = Resources.getTexture("guis/icons/icon_connected")
  private val unconnectedIcon = Resources.getTexture("guis/icons/icon_unconnected")

  final val MSG_FIND_NODES = "find_nodes"
  final val MSG_USER_CONNECT = "user_connect"
  final val MSG_USER_DISCONNECT = "unlink"
  final val MSG_NODE_CONNECT = "node_connect"
  final val MSG_NODE_DISCONNECT = "node_disconnect"
  final val MSG_FIND_NETWORKS = "find_networks"

  trait Target {
    def name: String
  }

  trait AvailTarget extends Target {
    def connect(pass: String)
  }

  trait LinkedTarget extends Target {
    def disconnect()
  }

  class LinkedInfo(var target: Option[LinkedTarget]) extends Component("LinkedInfo")

  private def rebuildPage(window: Widget, linked: Option[LinkedTarget], avail: Seq[AvailTarget]) = {
    val wlist = window.getWidget("panel_wireless/zone_elementlist")
    wlist.removeComponent("ElementList")

    val elist = new ElementList
    val elemTemplate = wlist.getWidget("element")

    {
      val connectElem = window.getWidget("panel_wireless/elem_connected")

      val (icon, name, alpha, tintEnabled) = linked match {
        case Some(target) => (connectedIcon, target.name, 1.0, true)
        case None => (unconnectedIcon, "Not Connected", 0.6, false)
      }

      connectElem.child("icon_connect").component[DrawTexture].texture = icon
      connectElem.child("icon_connect").component[DrawTexture].color.a = alpha
      connectElem.child("icon_connect").component[Tint].enabled = tintEnabled
      connectElem.child("icon_logo").component[DrawTexture].color.a = alpha
      connectElem.child("text_name").component[TextBox].setContent(name)

      connectElem.child("icon_connect").component[LinkedInfo].target = linked
    }

    avail.foreach(target => {
      val instance = elemTemplate.copy()

      val passBox = instance.getWidget("input_pass")
      val iconKey = instance.getWidget("icon_key")

      def confirm() = {
        val password = passBox.component[TextBox].content
        target.connect(password)
      }

      instance.getWidget("text_name").component[TextBox].setContent(target.name)

      passBox.listens[ConfirmInputEvent](() => confirm())
      instance.getWidget("icon_connect").listens[LeftClickEvent](() => confirm())

      passBox.listens[GainFocusEvent](() => iconKey.component[DrawTexture].color.a = 1.0)
      passBox.listens[LostFocusEvent](() => {
        passBox.component[TextBox].setContent("")
        iconKey.component[DrawTexture].color.a = 0.6
      })

      elist.addWidget(instance)
    })

    wlist :+ elist
  }

  def nodePage(node: TileNode): Page = {
    val ret = WirelessPage()

    val world = node.getWorldObj

    def rebuild(): Unit = {
      def newFuture() = Future.create((_: Boolean) => rebuild())

      send(MSG_FIND_NETWORKS, node, Future.create((data: NodeResult) => {
        val linked = Option(data.linked).map(matrix => new LinkedTarget {
          override def disconnect() = {
            send(MSG_NODE_DISCONNECT, node, newFuture())
          }
          override def name = matrix.ssid
        })
        val avail = data.avail.map(matrix => new AvailTarget {
            override def connect(pass: String): Unit = {
              send(MSG_NODE_CONNECT, node, matrix.ssid, pass, newFuture())
            }
            override def name: String = matrix.ssid
          })

        rebuildPage(ret.window, linked, avail)
      }))
    }

    rebuild()

    ret
  }

  def userPage(user: TileUser): Page = {
    val ret = WirelessPage()

    val world = user.getWorldObj

    def rebuild(): Unit = {
      def newFuture() = Future.create((result: Boolean) => rebuild())

      send(MSG_FIND_NODES, user, Future.create((result: UserResult) => {
        val linked = Option(result.linked).flatMap(_.tile(world)).map(node => new LinkedTarget {
          override def disconnect() = send(MSG_USER_DISCONNECT, user, newFuture())
          override def name: String = node.getNodeName
        })

        println(result.avail.toList)
        val avail = result.avail.toList.flatMap(_.tile(world)).map(node => new AvailTarget {
          override def connect(pass: String): Unit = send(MSG_USER_CONNECT, user, node, pass, newFuture())
          override def name: String = node.getNodeName
        })

        println(avail)

        rebuildPage(ret.window, linked, avail)
      }))
    }

    rebuild()

    ret
  }

  private def apply(): Page = {
    val widget = wirelessPageTemplate.copy()

    TechUI.breathe(widget.getWidget("icon_logo"))

    val wirelessPanel = widget.getWidget("panel_wireless")

    val wlist = wirelessPanel.getWidget("zone_elementlist")

    def elist = wlist.getComponent(classOf[ElementList])
    wirelessPanel.getWidget("btn_arrowup").listens[LeftClickEvent](() => elist.progressLast())
    wirelessPanel.getWidget("btn_arrowdown").listens[LeftClickEvent](() => elist.progressNext())

    val connectIcon = widget.child("panel_wireless/elem_connected/icon_connect")
    connectIcon :+ new LinkedInfo(None)
    connectIcon.listens[LeftClickEvent](() => connectIcon.component[LinkedInfo].target match {
      case Some(target) => target.disconnect()
      case _ =>
    })

    Page("wireless", widget)
  }

  private def send(msg: String, pars: Any*) = {
    NetworkMessage.sendToServer(WirelessNetDelegate, msg, pars.map(_.asInstanceOf[AnyRef]): _*)
  }

}

@Registrant
@NetworkS11nType
object WirelessNetDelegate {
  import WirelessPage._

  @RegInitCallback
  def __init() = {
    NetworkS11n.addDirectInstance(WirelessNetDelegate)
  }

  @Listener(channel=MSG_FIND_NODES, side=Array(Side.SERVER))
  private def hFindNodes(user: TileUser, fut: Future[UserResult]) = {
    def cvt(conn: NodeConn) = {
      val tile = conn.getNode.asInstanceOf[TileEntity]
      val ret = new NodeData
      ret.x = tile.xCoord
      ret.y = tile.yCoord
      ret.z = tile.zCoord
      ret
    }

    val linked = Option(WirelessHelper.getNodeConn(user))

    val nodes = WirelessHelper.getNodesInRange(user.getWorldObj,
      user.xCoord, user.yCoord, user.zCoord)
      .map(WirelessHelper.getNodeConn)
      .filter(!linked.contains(_))

    val data = new UserResult

    data.linked = linked.map(cvt).orNull
    data.avail = nodes.map(cvt).toArray

    fut.sendResult(data)
  }

  @Listener(channel=MSG_FIND_NETWORKS, side=Array(Side.SERVER))
  private def hFindNetworks(node: TileNode, fut: Future[NodeResult]) = {
    println("hFindNetworks")

    val linked = Option(WirelessHelper.getWirelessNet(node))

    def cvt(net: WirelessNet) = {
      val mat = net.getMatrix.asInstanceOf[TileEntity]
      val ret = new MatrixData()

      ret.x = mat.xCoord
      ret.y = mat.yCoord
      ret.z = mat.zCoord
      ret.ssid = net.getSSID

      ret
    }

    val networks = WirelessHelper.getNetInRange(node.getWorldObj,
      node.xCoord, node.yCoord, node.zCoord,
      node.getRange, 20)
      .filter(!linked.contains(_))

    val data = new NodeResult

    data.linked = linked.map(cvt).orNull
    data.avail = networks.map(cvt).toArray

    fut.sendResult(data)
  }

  @Listener(channel=MSG_USER_CONNECT, side=Array(Side.SERVER))
  private def hUserConnect(user: TileUser,
                       target: TileNode,
                       password: String,
                       fut: Future[Boolean]) = {
    val evt = new LinkUserEvent(user, target, password)
    val result = !MinecraftForge.EVENT_BUS.post(evt)

    fut.sendResult(result)
  }

  @Listener(channel=MSG_USER_DISCONNECT, side=Array(Side.SERVER))
  private def hUserDisconnect(user: TileBase, fut: Future[Boolean]) = {
    val evt = new UnlinkUserEvent(user)
    val result = !MinecraftForge.EVENT_BUS.post(evt)

    fut.sendResult(result)
  }

  @Listener(channel=MSG_NODE_CONNECT, side=Array(Side.SERVER))
  private def hNodeConnect(node: TileNode, ssid: String, pwd: String, fut: Future[Boolean]) = {
    val result = !MinecraftForge.EVENT_BUS.post(new LinkNodeEvent(node, ssid, pwd))
    fut.sendResult(result)
  }

  @Listener(channel=MSG_NODE_DISCONNECT, side=Array(Side.SERVER))
  private def hNodeDisconnect(node: TileNode, fut: Future[Boolean]) = {
    MinecraftForge.EVENT_BUS.post(new UnlinkNodeEvent(node))
    fut.sendResult(true)
  }

}

object InventoryPage {
  private val template = readxml("page_inv").getWidget("main")

  def apply(name: String): Page = {
    val ret = InventoryPage(template.copy())
    ret.window.getWidget("ui_block").component[DrawTexture].setTex(Resources.getTexture("guis/ui/ui_" + name))
    ret
  }

  def apply(ret: Widget): Page = {
    TechUI.breathe(ret.getWidget("ui_inv"))
    TechUI.breathe(ret.getWidget("ui_block"))

    Page("inv", ret)
  }

}