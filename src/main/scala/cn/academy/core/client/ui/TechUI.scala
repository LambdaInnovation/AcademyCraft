package cn.academy.core.client.ui

import java.util.function.Consumer

import cn.academy.Resources
import cn.lambdalib2.registry.StateEventCallback
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.lwjgl.util.Color
//import cn.academy.core.Resources
import cn.academy.core.client.ui.TechUI.Page
import cn.academy.energy.api.WirelessHelper
import cn.academy.energy.api.block.{IWirelessMatrix, IWirelessNode, IWirelessTile, IWirelessUser}
//import cn.academy.event.node.UnlinkUserEvent
import cn.academy.event.energy.{LinkNodeEvent, LinkUserEvent, UnlinkNodeEvent, UnlinkUserEvent}
import cn.academy.energy.impl.{NodeConn, WirelessNet}
import cn.academy.util.LocalHelper
import cn.lambdalib2.cgui.component.TextBox.{ChangeContentEvent, ConfirmInputEvent}
import cn.lambdalib2.cgui.{CGuiScreenContainer, Widget}
import cn.lambdalib2.cgui.component.ProgressBar.Direction
import cn.lambdalib2.cgui.component._
import cn.lambdalib2.cgui.component.Transform.{HeightAlign, WidthAlign}
import cn.lambdalib2.cgui.event.{FrameEvent, GainFocusEvent, LeftClickEvent, LostFocusEvent}
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.s11n.SerializeStrategy.ExposeStrategy
import cn.lambdalib2.s11n.{SerializeIncluded, SerializeNullable, SerializeStrategy}
import cn.lambdalib2.s11n.network.NetworkS11nType
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util._
import cn.lambdalib2.render.font.IFont.{FontAlign, FontOption}
import net.minecraftforge.fml.relauncher.Side
import net.minecraft.inventory.{Container, Slot}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import cn.lambdalib2.cgui.ScalaCGUI._
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge

import scala.collection.JavaConversions._
import org.lwjgl.opengl.GL11._

import collection.mutable

private object Generic_ {
  def readxml(loc: String) = CGUIDocument.read(new ResourceLocation(s"academy:guis/rework/$loc.xml"))
}

import Generic_._
import cn.academy.Resources.newTextBox

object TechUI {

  private lazy val pageButtonTemplate = readxml("pageselect").getWidget("main")
  private val blendQuadTex = Resources.getTexture("guis/blend_quad")
  private val histogramTex = Resources.getTexture("guis/histogram")
  private val lineTex = Resources.getTexture("guis/line")

  val local = LocalHelper.at("ac.gui.common")
  val localSep = local.subPath("sep")
  val localHist = local.subPath("hist")
  val localProperty = local.subPath("prop")

  case class Page(id: String, window: Widget)

  class BlendQuad(var margin: Double = 4) extends Component("BlendQuad") {

    val color = Colors.monoBlend(0.0f, 0.5f)

    this.listens[FrameEvent](() => {
      RenderUtils.loadTexture(blendQuadTex)
      Colors.bindToGL(color)

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
                  x: Float, y: Float,
                  limit: Float = Float.MaxValue) = {
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
          tex.color.setAlpha(Colors.f2i(breatheAlpha))
        })
      case _ =>
    }
  }

  /**
    * A global alpha value generator for producing uniform breathing effect.
    */
  def breatheAlpha = {
    val time = GameTimer.getTime
    val sin = (1 + math.sin(time / 0.8)) * 0.5

    (0.675 + sin * 0.175).toFloat
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
      button.scale(0.7f)
      button.pos(-20, idx * 22)
      button.listens[LeftClickEvent](() => {
        pages.foreach(_.window.transform.doesDraw = false)
        page.window.transform.doesDraw = true
        currentPage_ = page
      })
      button.listens((evt: FrameEvent) => {
        val a1 = if (evt.hovering || currentPage_ == page) 1.0f else 0.8f
        val a2 = if (currentPage_ == page) 1.0f else 0.8f
        buttonTex.color.setAlpha(Colors.f2i(a1))
        buttonTex.color.setRed(Colors.f2i(a2))
        buttonTex.color.setGreen(Colors.f2i(a2))
        buttonTex.color.setBlue(Colors.f2i(a2))
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
    val color = Colors.fromHexColor(0xff25c4ff)
    HistElement(localHist.get("energy"), color, () => energy() / max, () => "%.0f IF".format(energy()))
  }

  def histBuffer(energy: () => Double, max: Double) = {
    val color = Colors.fromHexColor(0xff25f7ff)
    HistElement(localHist.get("buffer"), color, () => energy() / max, () => "%.0f IF".format(energy()))
  }

  def histPhaseLiquid(amt: () => Double, max: Double) = {
    val color = Colors.fromHexColor(0xff7680de)
    HistElement(localHist.get("liquid"), color, () => amt() / max, () => "%.0f mB".format(amt()))
  }

  def histCapacity(amt: () => Int, max: => Int) = {
    val color = Colors.fromHexColor(0xffff6c00)
    HistElement(localHist.get("capacity"), color, () => amt().toDouble / max, () => s"${amt()}/$max")
  }

  class ContainerUI(container: Container, pages: Page*) extends CGuiScreenContainer(container) {
    xSize += 31
    ySize += 20

    class InfoArea extends Widget {
      this :+ new BlendQuad()

      private val keyLength = 40

      private val expectWidth = 100.0f
      private var expectHeight = 50.0f

      this.size(expectWidth, 0)

      private var lastFrameTime = GameTimer.getTime
      private val blendStartTime = GameTimer.getTime

      this.listens[FrameEvent](() => {
        val dt = math.min(GameTimer.getTime - lastFrameTime, 0.5)
        def move(fr: Float, to: Float): Float = {
          val max: Float = dt.toFloat * 500
          val delta = to - fr
          fr + math.min(max, math.abs(delta)) * math.signum(delta)
        }
        transform.width = move(transform.width, expectWidth)
        transform.height = move(transform.height, expectHeight)

        val balpha: Float = MathUtils.clampd(0, 1, (GameTimer.getTime - blendStartTime - 0.3) / 0.3).toFloat
        uas foreach (ua => ua(balpha))

        lastFrameTime = GameTimer.getTime
      })

      private var elemY: Float = 10
      private val elements = mutable.ArrayBuffer[Widget]()

      def histogram(elems: HistElement*) = {
        val widget = new Widget().size(210, 210).scale(0.4f)
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

        elems foreach histProperty

        this
      }

      def sepline(id: String) = {
        val widget = new Widget(expectWidth - 3, 8).pos(3, 0)
        widget :+ blend(newTextBox(new FontOption(6, Colors.monoBlend(1, 0.6f)))).setContent(localSep.get(id))

        blank(3)
        element(widget)

        this
      }

      def seplineInfo() = sepline("info")

      def button(name: String, callback: () => Any) = {
        val textBox = newTextBox(new FontOption(9, FontAlign.CENTER)).setContent(name)
        val len = textBox.font.getTextWidth(name, textBox.option)

        val widget = new Widget().walign(WidthAlign.CENTER).size(math.max(50, len + 5), 8)
        widget.listens((evt: FrameEvent) => {
          val lum = if (evt.hovering) 1.0f else 0.8f
          val color = textBox.option.color
          color.setRed(Colors.f2i(lum))
          color.setGreen(Colors.f2i(lum))
          color.setBlue(Colors.f2i(lum))
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
        val (idleColor, editColor) = (Colors.fromHexColor(0xffffffff), Colors.fromHexColor(0xff2180d8))

        val textBox = blend(newTextBox(new FontOption(8))).setContent(value.toString)
        val valueArea = new Widget().size(40, 8).halign(HeightAlign.CENTER)

        if (editCallback != null) {
          textBox.allowEdit = true
          textBox.option.color.setColor(idleColor)
          valueArea.listens[ConfirmInputEvent](() => {
            if (colorChange) {
              textBox.option.color.setColor(idleColor)
            }
            editCallback(textBox.content)
          })
          valueArea.listens[ChangeContentEvent](() => {
            if (colorChange) {
              textBox.option.color.setColor(editColor)
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
          .halign(HeightAlign.CENTER).pos(-3, .5f)
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
          .addComponent(blend(newTextBox(new FontOption(8))).setContent(localProperty.get(key)))
        value.pos(keyLength, 0)

        widget :+ keyArea
        widget :+ value

        element(widget)
      }

      def element(elem: Widget) = {
        elem.transform.y = elemY
        elemY += elem.transform.height * elem.transform.scale

        elements += elem
        expectHeight = math.max(50.0f, elemY + 8)
        this :+ elem

        this
      }

      def blank(ht: Double) = {
        elemY += ht.toFloat

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
        def apply(alpha: Float): Unit = us foreach (x => apply(x, alpha))
        def apply(obj: T, alpha: Float): Unit
      }
      private implicit object DrawTexUpdater extends Updater[DrawTexture] {
        override def apply(obj: DrawTexture, alpha: Float) = obj.color.setAlpha(Colors.f2i(alpha))
      }
      private implicit object TextBoxUpdater extends Updater[TextBox] {
        override def apply(obj: TextBox, alpha: Float) = obj.option.color.setAlpha(Colors.f2i(alpha))
      }
      private implicit object ProgressBarUpdater extends Updater[ProgressBar] {
        override def apply(obj: ProgressBar, alpha: Float) = obj.color.setAlpha(Colors.f2i(alpha))
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

@NetworkS11nType
private class UserResult {
  @SerializeIncluded
  @SerializeNullable
  var linked: NodeData = null
  @SerializeIncluded
  var avail: Array[NodeData] = null
}

@NetworkS11nType
private class NodeResult {
  @SerializeIncluded
  @SerializeNullable
  var linked: MatrixData = null
  @SerializeIncluded
  var avail: Array[MatrixData] = null
}

@NetworkS11nType
@SerializeStrategy(strategy=ExposeStrategy.ALL)
private class MatrixData {
  var x: Int = 0
  var y: Int = 0
  var z: Int = 0
  var ssid: String = null
  var encrypted: Boolean = false

  def tile(world: World) = world.getTileEntity(new BlockPos(x, y, z)) match {
    case tile: IWirelessMatrix => Some(tile)
    case _ => None
  }
}

@NetworkS11nType
@SerializeStrategy(strategy=ExposeStrategy.ALL)
private class NodeData {
  var x: Int = 0
  var y: Int = 0
  var z: Int = 0
  var encrypted: Boolean = false

  def tile(world: World) = world.getTileEntity(new BlockPos(x, y, z)) match {
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

  private val local = TechUI.local.subPath("pg_wireless")

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
    wlist.removeComponent(classOf[ElementList])

    val elist = new ElementList
    wlist.getWidget("element").transform.doesDraw = false
    val elemTemplate = wlist.getWidget("element").copy()

    {
      val connectElem = window.getWidget("panel_wireless/elem_connected")

      val (icon, name, alpha, tintEnabled) = linked match {
        case Some(target) => (connectedIcon, target.name, 1.0f, true)
        case None => (unconnectedIcon, local.get("not_connected"), 0.6f, false)
      }

      connectElem.child("icon_connect").component[DrawTexture].texture = icon
      connectElem.child("icon_connect").component[DrawTexture].color.setAlpha(Colors.f2i(alpha))
      connectElem.child("icon_connect").component[Tint].enabled = tintEnabled
      connectElem.child("icon_logo").component[DrawTexture].color.setAlpha(Colors.f2i(alpha))
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

        passBox.component[TextBox].setContent("")
      }

      instance.getWidget("text_name").component[TextBox].setContent(target.name)

      if (target.encrypted) {
        passBox.listens[ConfirmInputEvent](() => confirm())
        passBox.listens[GainFocusEvent](() => iconKey.component[DrawTexture].color.setAlpha(Colors.f2i(1.0f)))
        passBox.listens[LostFocusEvent](() => {
          iconKey.component[DrawTexture].color.setAlpha(Colors.f2i(0.6f))
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

    val world = node.getWorld

    def rebuild(): Unit = {
      def newFuture() = Future.create(new Consumer[Boolean]{
        override def accept(b: Boolean) = rebuild()
      })

      send(MSG_FIND_NETWORKS, node, Future.create(new Consumer[NodeResult]{
        override def accept(data: NodeResult) = {
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
        }
      }))
    }

    rebuild()

    ret.window.child("icon_logo").component[DrawTexture].setTex(toMatrixIcon)

    ret
  }

  def userPage(user: TileUser): Page = {
    val ret = WirelessPage()

    val world = user.getWorld

    def rebuild(): Unit = {
      def newFuture() = Future.create2((result: Boolean) => rebuild())

      send(MSG_FIND_NODES, user, Future.create2((result: UserResult) => {
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

object WirelessNetDelegate {
  import WirelessPage._

  @StateEventCallback
  def __init(ev: FMLInitializationEvent) = {
    NetworkS11n.addDirectInstance(WirelessNetDelegate)
  }

  @Listener(channel=MSG_FIND_NODES, side=Array(Side.SERVER))
  private def hFindNodes(user: TileUser, fut: Future[UserResult]) = {
    def cvt(conn: NodeConn) = {
      val tile = conn.getNode.asInstanceOf[TileNode]
      val ret = new NodeData
      ret.x = tile.getPos.getX
      ret.y = tile.getPos.getY
      ret.z = tile.getPos.getZ
      ret.encrypted = !tile.getPassword.isEmpty
      ret
    }

    val linked = Option(WirelessHelper.getNodeConn(user))

    val nodes = WirelessHelper.getNodesInRange(user.getWorld, user.getPos)
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

      ret.x = mat.getPos.getX
      ret.y = mat.getPos.getY
      ret.z = mat.getPos.getZ
      ret.ssid = net.getSSID
      ret.encrypted = !net.getPassword.isEmpty

      ret
    }

    val networks = WirelessHelper.getNetInRange(node.getWorld,
      node.getPos.getX, node.getPos.getY, node.getPos.getZ,
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