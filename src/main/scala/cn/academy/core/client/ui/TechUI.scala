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
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.client.font.IFont.{FontAlign, FontOption}
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.helper.{GameTimer, Color}
import cpw.mods.fml.relauncher.Side
import net.minecraft.inventory.Container
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import cn.lambdalib.cgui.ScalaCGUI._
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import scala.collection.JavaConversions._
import org.lwjgl.opengl.GL11._
import collection.mutable

private object Generic_ {
  def readxml(loc: String) = CGUIDocument.panicRead(new ResourceLocation(s"academy:guis/rework/$loc.xml"))
}

import Generic_._
import Resources.newTextBox

object TechUI {

  private lazy val pageButtonTemplate = readxml("pageselect").getWidget("main")
  private val blendQuadTex = Resources.getTexture("guis/blend_quad")
  private val histogramTex = Resources.getTexture("guis/histogram")
  private val lineTex = Resources.getTexture("guis/line")

  case class Page(id: String, window: Widget)

  class BlendQuad(var margin: Double = 4) extends Component("BlendQuad") {

    val color = Color.monoBlend(0.0, 0.5)

    this.listens[FrameEvent](() => {
      RenderUtils.loadTexture(blendQuadTex)
      color.bind()

      def quad(col: Int, row: Int, x0: Double, y0: Double, x1: Double, y1: Double) = {
        val u = col / 3.0
        val v = row / 3.0
        val step = 1.0 / 3.0

        glTexCoord2d(u, v)
        glVertex2d(x0, y0)

        glTexCoord2d(u, v + step)
        glVertex2d(x0, y1)

        glTexCoord2d(u + step, v + step)
        glVertex2d(x1, y1)

        glTexCoord2d(u + step, v)
        glVertex2d(x1, y0)
      }

      val (x, y, w, h) = (0, 0, widget.transform.width, widget.transform.height)
      val xs = Array(x - margin, x, x + w, x + w + margin)
      val ys = Array(y - margin, y, y + h, y + h + margin)

      glBegin(GL_QUADS)

      for {
        i <- 0 until 3
        j <- 0 until 3
      } quad(i, j, xs(i), ys(j), xs(i+1), ys(j+1))

      glEnd()

      glColor4d(1, 1, 1, 1)
      RenderUtils.loadTexture(lineTex)

      val mrg = 3.2
      HudUtils.rect(-mrg, -8.6, w + mrg*2, 12)
      HudUtils.rect(-mrg, h - 2, w + mrg*2, 8)
    })

  }

  def drawTextBox(content: String, option: FontOption,
                  x: Double, y: Double,
                  limit: Double = Double.MaxValue) = {
    val wmargin = 5
    val hmargin = 2

    val font = Resources.font()
    val extent = font.drawSeperated_Sim(content, limit, option)

    glColor4f(0, 0, 0, 0.5f)
    HudUtils.colorRect(x - extent.width * option.align.lenOffset, y,
      extent.width + wmargin * 2 + 2, extent.height + hmargin * 2)

    glColor4f(1, 1, 1, 0.8f)
    font.drawSeperated(content, x + wmargin, y + hmargin, limit, option)

    glColor4f(1, 1, 1, 1)
  }


  /**
    * Creates a tech UI with specified pages.
    *
    * @param pages The pages of this UI. Must not be empty.
    */
  def apply(pages: Page*) = new TechUIWidget(pages: _*)

  def breathe(widget: Widget) = {
    Option(widget.getComponent(classOf[DrawTexture])) match {
      case Some(tex) =>
        widget.listens[FrameEvent](() => {
          tex.color.a = breatheAlpha
        })
      case _ =>
    }
  }

  /**
    * A global alpha value generator for producing uniform breathing effect.
    */
  def breatheAlpha = {
    val time = GameTimer.getTime
    val sin = (1 + math.sin(time / 800.0)) * 0.5

    0.675 + sin * 0.175
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

  case class HistElement(id: String, color: Color,
                         value: () => Double, desc: () => String)

  def histEnergy(energy: () => Double, max: Double) = {
    val color = new Color(0xff25c4ff)
    HistElement("ENERGY", color, () => energy() / max, () => "%.0f IF".format(energy()))
  }

  def histBuffer(energy: () => Double, max: Double) = {
    val color = new Color(0xff25f7ff)
    HistElement("BUFFER", color, () => energy() / max, () => "%.0f IF".format(energy()))
  }

  class ContainerUI(container: Container, pages: Page*) extends CGuiScreenContainer(container) {
    class InfoArea extends Widget {
      this :+ new BlendQuad()

      private val keyLength = 40

      private val expectWidth = 100.0
      private var expectHeight = 50.0

      this.size(expectWidth, 0)

      private var lastFrameTime = GameTimer.getTime
      private val blendStartTime = GameTimer.getTime

      this.listens[FrameEvent](() => {
        val dt = math.min(GameTimer.getTime - lastFrameTime, 500) / 1000.0
        def move(fr: Double, to: Double): Double = {
          val max = dt * 500
          val delta = to - fr
          fr + math.min(max, math.abs(delta)) * math.signum(delta)
        }
        transform.width = move(transform.width, expectWidth)
        transform.height = move(transform.height, expectHeight)

        val balpha = MathUtils.clampd(0, 1, (GameTimer.getTime - blendStartTime - 300) / 300.0)
        uas foreach (ua => ua(balpha))

        lastFrameTime = GameTimer.getTime
      })

      private var elemY: Double = 10
      private val elements = mutable.ArrayBuffer[Widget]()

      def histogram(elems: HistElement*) = {
        val widget = new Widget().size(210, 210).scale(0.4)
            .addComponent(blend(new DrawTexture(histogramTex)))

        elems.zipWithIndex.foreach { case (elem, idx) => {
          val bar = new Widget().size(16, 120).pos(56 + idx * 40, 78)
          val progress = blend(new ProgressBar)
          progress.color = elem.color
          progress.dir = Direction.UP

          bar.listens[FrameEvent](() => {
            progress.progress = MathUtils.clampd(0.03, 1, elem.value())
          })
          bar :+ progress

          widget :+ bar
        }}

        blank(-30)
        element(widget)

        elems foreach (histProperty)

        this
      }

      def sepline(id: String) = {
        val widget = new Widget(expectWidth - 3, 8).pos(3, 0)
        widget :+ blend(newTextBox(new FontOption(6, Color.monoBlend(1, 0.6)))).setContent(id)

        blank(3)
        element(widget)

        this
      }

      def button(name: String, callback: () => Any) = {
        val textBox = newTextBox(new FontOption(9, FontAlign.CENTER)).setContent(name)
        val len = textBox.font.getTextWidth(name, textBox.option)

        val widget = new Widget().walign(WidthAlign.CENTER).size(math.max(50, len + 5), 8)
        widget.listens((evt: FrameEvent) => {
          val lum = if (evt.hovering) 1.0 else 0.8
          val color = textBox.option.color
          color.r = lum
          color.g = lum
          color.b = lum
        })
        widget.listens[LeftClickEvent](callback)
        widget :+ blend(textBox)

        element(widget)
      }

      def property[T](key: String, value: =>T,
                      editCallback: String => Any = null,
                      password: Boolean = false,
                      colorChange: Boolean = true,
                      contentCell: Array[TextBox] = null) = { // Content cell is a temp hack to get the text component
        val (idleColor, editColor) = (new Color(0xffffffff), new Color(0xff2180d8))

        val textBox = blend(newTextBox(new FontOption(8))).setContent(value.toString)
        val valueArea = new Widget().size(40, 8).halign(HeightAlign.CENTER)

        if (editCallback != null) {
          textBox.allowEdit = true
          textBox.option.color.from(idleColor)
          valueArea.listens[ConfirmInputEvent](() => {
            if (colorChange) {
              textBox.option.color.from(idleColor)
            }
            editCallback(textBox.content)
          })
          valueArea.listens[ChangeContentEvent](() => {
            if (colorChange) {
              textBox.option.color.from(editColor)
            }
          })

          def box(ch: String) = {
            val ret = new Widget().size(10, 8)
              .halign(HeightAlign.CENTER)
              .addComponent(blend(newTextBox(new FontOption(8)).setContent(ch)))
            ret
          }

          val (box0, box1) = (box("[").pos(-4, 0), box("]").pos(valueArea.transform.width + 2, 0))
          box0.transform.doesListenKey = false
          box1.transform.doesListenKey = false
          valueArea :+ box0
          valueArea :+ box1
        } else {
          valueArea.listens[FrameEvent](() => textBox.content = value.toString)
        }

        if (password) {
          textBox.doesEcho = true
        }

        valueArea :+ textBox

        kvpair(key, valueArea)

        if (contentCell != null) {
          contentCell.update(0, textBox)
        }

        this
      }

      private def histProperty(elem: HistElement) = {
        val widget = new Widget(expectWidth - 10, 8).pos(6, 0)

        val keyArea = new Widget().pos(4, 0).size(32, 8).halign(HeightAlign.CENTER)
          .addComponent(blend(newTextBox(new FontOption(8))).setContent(elem.id))
        val icon = new Widget().size(6, 6)
          .halign(HeightAlign.CENTER).pos(-3, .5)
          .addComponents(blend(new DrawTexture(null).setColor(elem.color)))
        val valueArea = new Widget().pos(keyLength, 0).size(40, 8).halign(HeightAlign.CENTER)
          .addComponent(blend(newTextBox(new FontOption(8))).setContent(elem.desc()))

        valueArea.listens[FrameEvent](() => {
          valueArea.component[TextBox].content = elem.desc()
        })

        widget :+ keyArea
        widget :+ icon
        widget :+ valueArea

        element(widget)
      }

      private def kvpair(key: String, value: Widget) = {
        val widget = new Widget(expectWidth - 10, 8).pos(6, 0)
        val keyArea = new Widget().size(40, 8).halign(HeightAlign.CENTER)
          .addComponent(blend(newTextBox(new FontOption(8))).setContent(key))
        value.pos(keyLength, 0)

        widget :+ keyArea
        widget :+ value

        element(widget)
      }

      def element(elem: Widget) = {
        elem.transform.y = elemY
        elemY += elem.transform.height * elem.transform.scale

        elements += elem
        expectHeight = math.max(50.0, elemY + 8)
        this :+ elem

        this
      }

      def blank(ht: Double) = {
        elemY += ht

        this
      }

      def reset() = {
        elements foreach (_.dispose())
        elements.clear()
        elemY = 10
        uas foreach (_.clear())

        this
      }

      private trait Updater[T] {
        private val us = mutable.ArrayBuffer[T]()

        def add(obj: T) = us += obj
        def clear() = us.clear()
        def apply(alpha: Double): Unit = us foreach (x => apply(x, alpha))
        def apply(obj: T, alpha: Double): Unit
      }
      private implicit object DrawTexUpdater extends Updater[DrawTexture] {
        override def apply(obj: DrawTexture, alpha: Double) = obj.color.a = alpha
      }
      private implicit object TextBoxUpdater extends Updater[TextBox] {
        override def apply(obj: TextBox, alpha: Double) = obj.option.color.a = alpha
      }
      private implicit object ProgressBarUpdater extends Updater[ProgressBar] {
        override def apply(obj: ProgressBar, alpha: Double) = obj.color.a = alpha
      }
      private val uas = List(DrawTexUpdater, TextBoxUpdater, ProgressBarUpdater)

      private def blend[T](obj: T)(implicit ua: Updater[T]) = {
        ua.add(obj)
        obj
      }

    }

    val main = TechUI(pages: _*)
    main.pos(-18, 0)

    val infoPage = new InfoArea()
    infoPage.pos(main.transform.width + 7, 5)

    main :+ infoPage

    gui.addWidget(main)

    def shouldDisplayInventory(page: Page): Boolean = page.id == "inv"

    override def isSlotActive = shouldDisplayInventory(main.currentPage)
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
  var encrypted: Boolean = false

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
  var encrypted: Boolean = false

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

  private lazy val wirelessPageTemplate = readxml("page_wireless").getWidget("main")

  private val connectedIcon = Resources.getTexture("guis/icons/icon_connected")
  private val unconnectedIcon = Resources.getTexture("guis/icons/icon_unconnected")
  private val toMatrixIcon = Resources.getTexture("guis/icons/icon_tomatrix")

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
    def encrypted: Boolean
  }

  trait LinkedTarget extends Target {
    def disconnect()
  }

  class LinkedInfo(var target: Option[LinkedTarget]) extends Component("LinkedInfo")

  private def rebuildPage(window: Widget, linked: Option[LinkedTarget], avail: Seq[AvailTarget]) = {
    val wlist = window.getWidget("panel_wireless/zone_elementlist")
    wlist.removeComponent("ElementList")

    val elist = new ElementList
    wlist.getWidget("element").transform.doesDraw = false
    val elemTemplate = wlist.getWidget("element").copy()

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

      if (target.encrypted) {
        passBox.listens[ConfirmInputEvent](() => confirm())
        passBox.listens[GainFocusEvent](() => iconKey.component[DrawTexture].color.a = 1.0)
        passBox.listens[LostFocusEvent](() => {
          passBox.component[TextBox].setContent("")
          iconKey.component[DrawTexture].color.a = 0.6
        })
      } else {
        Array(passBox, iconKey).foreach(_.transform.doesDraw = false)
      }

      instance.getWidget("icon_connect").listens[LeftClickEvent](() => confirm())

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
        val avail = data.avail
            .map(matrix => (matrix, matrix.tile(world)))
            .map {
              case (matrix, Some(tile)) =>
                new AvailTarget {
                  override def connect(pass: String): Unit = {
                    send(MSG_NODE_CONNECT, node, tile, pass, newFuture())
                  }
                  override def name: String = matrix.ssid
                  override def encrypted = matrix.encrypted
                }
            }

        rebuildPage(ret.window, linked, avail)
      }))
    }

    rebuild()

    ret.window.child("icon_logo").component[DrawTexture].setTex(toMatrixIcon)

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

        val avail = result.avail.toList.map(a => (a.tile(world), a.encrypted))
          .flatMap {
          case (Some(node), enc) =>
            Some[AvailTarget](new AvailTarget {
              override def connect(pass: String): Unit = send(MSG_USER_CONNECT, user, node, pass, newFuture())
              override def name: String = node.getNodeName
              override def encrypted = enc
            })
          case _ => None
          }

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
      val tile = conn.getNode.asInstanceOf[TileNode]
      val ret = new NodeData
      ret.x = tile.xCoord
      ret.y = tile.yCoord
      ret.z = tile.zCoord
      ret.encrypted = !tile.getPassword.isEmpty
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
    val linked = Option(WirelessHelper.getWirelessNet(node))

    def cvt(net: WirelessNet) = {
      val mat = net.getMatrix.asInstanceOf[TileEntity]
      val ret = new MatrixData()

      ret.x = mat.xCoord
      ret.y = mat.yCoord
      ret.z = mat.zCoord
      ret.ssid = net.getSSID
      ret.encrypted = !net.getPassword.isEmpty

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
  private def hNodeConnect(node: TileNode, mat: TileMatrix, pwd: String, fut: Future[Boolean]) = {
    val result = !MinecraftForge.EVENT_BUS.post(new LinkNodeEvent(node, mat, pwd))
    fut.sendResult(result)
  }

  @Listener(channel=MSG_NODE_DISCONNECT, side=Array(Side.SERVER))
  private def hNodeDisconnect(node: TileNode, fut: Future[Boolean]) = {
    MinecraftForge.EVENT_BUS.post(new UnlinkNodeEvent(node))
    fut.sendResult(true)
  }

}

object InventoryPage {
  private lazy val template = readxml("page_inv").getWidget("main")

  def apply(name: String): Page = {
    val ret = InventoryPage(template.copy())
    ret.window.getWidget("ui_block").component[DrawTexture].setTex(Resources.getTexture("guis/ui/ui_" + name))
    ret
  }

  def apply(ret: Widget): Page = {
    ret.getDrawList.foreach {
      case w if w.getName.startsWith("ui_") => TechUI.breathe(w)
      case _ =>
    }

    Page("inv", ret)
  }

}