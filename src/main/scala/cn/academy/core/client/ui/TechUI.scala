package cn.academy.core.client.ui

import cn.academy.core.client.Resources
import cn.academy.core.client.ui.TechUI.Page
import cn.academy.energy.api.WirelessHelper
import cn.academy.energy.api.block.{IWirelessNode, IWirelessUser, IWirelessTile}
import cn.academy.energy.api.event.node.{UnlinkUserEvent, LinkUserEvent}
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.cgui.gui.component.TextBox.ConfirmInputEvent
import cn.lambdalib.cgui.gui.{CGuiScreenContainer, Widget}
import cn.lambdalib.cgui.gui.component.ProgressBar.Direction
import cn.lambdalib.cgui.gui.component._
import cn.lambdalib.cgui.gui.component.Transform.{HeightAlign, WidthAlign}
import cn.lambdalib.cgui.gui.event.{GainFocusEvent, LostFocusEvent, FrameEvent, LeftClickEvent}
import cn.lambdalib.cgui.xml.CGUIDocument
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
import net.minecraftforge.common.MinecraftForge
import scala.collection.JavaConversions._

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
      val time = GameTimer.getTime
      val sin = (1 + math.sin(time / 500.0)) * 0.5
      tex.color.a = 0.5 + sin * 0.35
    })
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

  private val configPageTemplate = readxml("page_config").getWidget("main")

  case class HistoElement(id: String, color: Color, progressProvider: () => Double)

  def textProperty(content: String, color: Color = Color.white()) = {
    val ret = new Widget().size(142, 12)
    ret :+ new TextBox(new FontOption(10, color)).setContent(content)
    ret
  }

  def textPropertyUpdated(contentProvider: () => String, color: Color = Color.white()) = {
    val ret = new Widget().size(142, 12)
    val textBox = new TextBox(new FontOption(10, color)).setContent(contentProvider())
    ret :+ textBox
    ret.listens[FrameEvent](() => textBox.setContent(contentProvider()))

    ret
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
        bar.listens[FrameEvent](() => {
          progress.progress = elem.progressProvider()
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
private class UserData {
  @SerializeIncluded
  var nodes: Array[Array[Int]] = null
  @SerializeIncluded
  @SerializeNullable
  var connected: TileEntity with IWirelessNode = null
}

object WirelessPage {
  type TileUser = TileEntity with IWirelessUser
  type TileNode = TileEntity with IWirelessNode
  type TileBase = TileEntity with IWirelessTile

  private val wirelessPageTemplate = readxml("page_wireless").getWidget("main")

  private val connectedIcon = Resources.getTexture("guis/icons/icon_connected")
  private val unconnectedIcon = Resources.getTexture("guis/icons/icon_unconnected")

  final val MSG_FIND_NODES = "find_nodes"
  final val MSG_USER_CONNECT = "user_connect"
  final val MSG_UNLINK = "unlink"

  def apply(user: TileUser) = {
    val page = createPage()

    val connectIcon = page.window.getWidget("panel_wireless/elem_connected/icon_connect")
    connectIcon.listens[LeftClickEvent](() => {
      NetworkMessage.sendToServer(WirelessNetDelegate, MSG_UNLINK, user, Future.create((res: Boolean) => {
        gatherUserInfo(page.window, user)
      }))
    })

    gatherUserInfo(page.window, user)

    page
  }

  private def createPage() = {
    val widget = wirelessPageTemplate.copy()

    TechUI.breathe(widget.getWidget("icon_logo"))

    val wirelessPanel = widget.getWidget("panel_wireless")

    val wlist = wirelessPanel.getWidget("zone_elementlist")

    def elist = wlist.getComponent(classOf[ElementList])
    wirelessPanel.getWidget("btn_arrowup").listens[LeftClickEvent](() => elist.progressLast())
    wirelessPanel.getWidget("btn_arrowdown").listens[LeftClickEvent](() => elist.progressNext())

    Page("wireless", widget)
  }

  private def rebuildElements(widget: Widget, rebuildr: (ElementList, Widget) => Any) = {
    val wlist = widget.getWidget("panel_wireless/zone_elementlist")
    wlist.removeComponent("ElementList")

    val elist = new ElementList
    rebuildr(elist, wlist.getWidget("element"))

    wlist :+ elist
  }

  private def gatherUserInfo(window: Widget, tile: TileUser): Unit = {
    val fut = Future.create((inp: UserData) => {
      println("Received results " + inp.connected + ", " + inp.nodes)
      val nodes = inp.nodes.flatMap(arr => {
        val world = tile.getWorldObj
        val (x, y, z) = (arr(0), arr(1), arr(2))
        val subtile = world.getTileEntity(x, y, z)
        subtile match {
          case node: IWirelessNode if node != inp.connected => Some(node)
          case _ => None
        }
      })

      val connectElem = window.getWidget("panel_wireless/elem_connected")
      setConnectState(connectElem, inp.connected)

      rebuildElements(window, (list, template) => {
        nodes.foreach(node => {
          val instance = template.copy()

          val passBox = instance.getWidget("input_pass")
          val iconKey = instance.getWidget("icon_key")
          def confirm() = {
            val future = Future.create((result: Boolean) => {
              gatherUserInfo(window, tile)
            })
            NetworkMessage.sendToServer(WirelessNetDelegate, MSG_USER_CONNECT,
              tile, node, passBox.component[TextBox].content, future)
          }

          instance.getWidget("text_name").component[TextBox].setContent(node.getNodeName)
          passBox.listens[ConfirmInputEvent](() => confirm())

          passBox.listens[GainFocusEvent](() => iconKey.component[DrawTexture].color.a = 1.0)
          passBox.listens[LostFocusEvent](() => {
            passBox.component[TextBox].setContent("")
            iconKey.component[DrawTexture].color.a = 0.6
          })

          instance.getWidget("icon_connect").listens[LeftClickEvent](() => confirm())

          list.addWidget(instance)
        })
      })
    })
    println("findNodes")
    NetworkMessage.sendToServer(WirelessNetDelegate, MSG_FIND_NODES, tile, fut)
  }

  private def setConnectState(element: Widget, targ: IWirelessNode) = {
    val state = targ != null
    element.getWidget("icon_connect").component[DrawTexture].setTex(if (state) connectedIcon else unconnectedIcon)
    element.getWidget("icon_connect").component[DrawTexture].color.a = if (state) 1.0 else 0.6
    element.getWidget("icon_connect").component[Tint].enabled = state
    element.getWidget("icon_logo").component[DrawTexture].color.a = if (state) 1.0 else 0.6
    element.getWidget("text_name").component[TextBox].content = if (state) targ.getNodeName else ""
  }

}

@Registrant
@NetworkS11nType
object WirelessNetDelegate {
  import WirelessPage._

  NetworkS11n.addDirectInstance(this)

  @Listener(channel=MSG_FIND_NODES, side=Array(Side.SERVER))
  private def hFindNodes(user: TileUser, fut: Future[UserData]) = {
    val nodes = WirelessHelper.getNodesInRange(user.getWorldObj,
      user.xCoord, user.yCoord, user.zCoord)
      .map(node => {
        val tile = node.asInstanceOf[TileEntity]
        Array(tile.xCoord, tile.yCoord, tile.zCoord)
      }).toArray
    println("hFindNdoes")

    val data = new UserData
    data.connected = Option(WirelessHelper.getNodeConn(user))
      .map(_.getNode)
      .orNull.asInstanceOf[TileNode]
    data.nodes = nodes

    fut.sendResult(data)
  }

  @Listener(channel=MSG_USER_CONNECT, side=Array(Side.SERVER))
  private def hConfirm(user: TileUser,
                       target: TileNode,
                       password: String,
                       fut: Future[Boolean]) = {
    val evt = new LinkUserEvent(user, target, password)
    val result = !MinecraftForge.EVENT_BUS.post(evt)

    fut.sendResult(result)
  }

  @Listener(channel=MSG_UNLINK, side=Array(Side.SERVER))
  private def hUnlink(user: TileBase, fut: Future[Boolean]) = {
    val evt = new UnlinkUserEvent(user)
    val result = !MinecraftForge.EVENT_BUS.post(evt)

    fut.sendResult(result)
  }

}

object InventoryPage {
  private val template = readxml("page_inv").getWidget("main")

  def apply(name: String) = {
    val ret = template.copy()

    ret.getWidget("ui_block").getComponent(classOf[DrawTexture]).setTex(Resources.getTexture("guis/ui/ui_" + name))

    TechUI.breathe(ret.getWidget("ui_inv"))
    TechUI.breathe(ret.getWidget("ui_block"))

    Page("inv", ret)
  }

}