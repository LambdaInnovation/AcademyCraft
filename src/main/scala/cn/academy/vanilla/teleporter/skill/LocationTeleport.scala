package cn.academy.vanilla.teleporter.skill

import java.util
import java.util.function.Predicate

import cn.academy.ability.api.{AbilityContext, Skill}
import cn.academy.ability.api.context.{ClientRuntime, KeyDelegate}
import cn.academy.ability.api.data.AbilityData
import cn.academy.core.Resources
import cn.academy.misc.achievements.ModuleAchievements
import cn.academy.vanilla.teleporter.util.TPSkillHelper
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.{CGuiScreen, Widget}
import cn.lambdalib.cgui.gui.component.{Component, DrawTexture, ElementList, TextBox}
import cn.lambdalib.cgui.gui.component.TextBox.ConfirmInputEvent
import cn.lambdalib.cgui.gui.event.{FrameEvent, IGuiEventHandler, LeftClickEvent}
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.s11n.{SerializeIncluded, SerializeStrategy}
import cn.lambdalib.s11n.SerializeStrategy.ExposeStrategy
import cn.lambdalib.s11n.nbt.NBTS11n
import cn.lambdalib.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType
import cn.lambdalib.util.client.HudUtils
import cn.lambdalib.util.client.font.IFont.{FontAlign, FontOption}
import cn.lambdalib.util.datapart.{DataPart, EntityData, RegDataPart}
import cn.lambdalib.util.generic.MathUtils
import cn.lambdalib.util.helper.{Color, GameTimer}
import cn.lambdalib.util.mc.{EntitySelectors, WorldUtils}
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{MathHelper, ResourceLocation, StatCollector}
import net.minecraftforge.common.DimensionManager

@Registrant
private object LTNetDelegate {
  final val MSG_ADD = "add"
  final val MSG_REMOVE = "remove"
  final val MSG_QUERY = "query"
  final val MSG_PERFORM = "perform"

  import scala.collection.JavaConversions._

  @RegInitCallback
  def _init() = {
    NetworkS11n.register(classOf[util.ArrayList[_]])
    NetworkS11n.addDirectInstance(LTNetDelegate)
  }

  def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(LTNetDelegate, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

  @Listener(channel=MSG_ADD, side=Array(Side.SERVER))
  private def hAdd(player: EntityPlayer, name: String, future: Future[util.List[Location]]) = {
    val data = LocTeleportData(player)
    data.add(name, player.worldObj.provider.dimensionId,
      (player.posX.toFloat, player.posY.toFloat, player.posZ.toFloat))
    future.sendResult(new util.ArrayList(data.locations))
  }

  @Listener(channel=MSG_REMOVE, side=Array(Side.SERVER))
  private def hRemove(player: EntityPlayer, id: Int, future: Future[util.List[Location]]) = {
    val data = LocTeleportData(player)
    data.remove(id)
    future.sendResult(new util.ArrayList(data.locations))
  }

  @Listener(channel=MSG_PERFORM, side=Array(Side.SERVER))
  private def hPerform(player: EntityPlayer, location: Location) = {
    LocationTeleport.perform(player, location)
  }

}

object LocationTeleport extends Skill("location_teleport", 3) {

  val teleportSelector = EntitySelectors.living.and(new Predicate[Entity] {
    override def test(t: Entity): Boolean = t.width * t.width * t.height < 80f
  })

  override def activate(rt: ClientRuntime, keyID: Int): Unit = {
    rt.addKey(keyID, new KeyDelegate {
      override def getIcon: ResourceLocation = getHintIcon

      override def createID(): Int = 0

      override def getSkill: Skill = LocationTeleport

      override def onKeyDown(): Unit = {
        getMC.displayGuiScreen(new Gui)
      }

    })
  }

  def canCrossDimension(player: EntityPlayer) = AbilityData.get(player).getSkillExp(this) > 0.8f

  /**
    * @return (Overload, CP) consumption
    */
  def getConsumption(player: EntityPlayer, dest: Location): (Float, Float) = {
    val data = AbilityData.get(player)
    val distance = player.getDistance(dest.x, dest.y, dest.z).toFloat
    val dimPenalty = if (isCrossDim(player, dest)) 2 else 1

    (240, MathUtils.lerpf(200, 150, data.getSkillExp(this)) * dimPenalty *
      math.max(8.0f, MathHelper.sqrt_float(math.min(800, distance))))
  }

  /**
    * @return `None` if can perform. `Some(reason)` if can't.
    */
  def getPerformStat(player: EntityPlayer, dest: Location): Option[String] = {
    def fail(id: String) = Some(StatCollector.translateToLocal("ac.gui.loctele." + id))

    if (isCrossDim(player, dest) && !canCrossDimension(player)) {
      fail("err_exp")
    } else {
      val ctx = AbilityContext.of(player, this)
      val (_, cp) = getConsumption(player, dest)
      if (ctx.canConsumeCP(cp)) {
        None
      } else {
        fail("err_cp")
      }
    }
  }

  def perform(player: EntityPlayer, dest: Location) = {
    import scala.collection.JavaConversions._

    val ctx = AbilityContext.of(player, this)

    val (o, cp) = getConsumption(player, dest)
    ctx.consumeWithForce(o, cp)

    val entitiesToTeleport: List[Entity] = player :: WorldUtils.getEntities(player, 5,
      teleportSelector.and(EntitySelectors.exclude(player))).toList

    if (isCrossDim(player, dest)) {
      entitiesToTeleport.foreach(_.travelToDimension(dest.dim))
    }

    val (px, py, pz) = (player.posX, player.posY, player.posZ)
    entitiesToTeleport.foreach(e => {
      val (dx, dy, dz) = (e.posX - px, e.posY - py, e.posZ - pz)
      if(e.isRiding())e.mountEntity(null)
      e.setPositionAndRotation(dest.x + dx, dest.y + dy, dest.z + dz, e.rotationYaw, e.rotationPitch);
    })

    player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ,
      "academy:tp.tp", 0.5f, 1.0f)

    val dist = player.getDistance(dest.x, dest.y, dest.z)
    val expincr = if (dist >= 200) 0.03f else 0.015f
    ctx.addSkillExp(expincr)
    ctx.setCooldown(MathUtils.lerpf(30, 20, ctx.getSkillExp).toInt)

    ModuleAchievements.trigger(player, "teleporter.ignore_barrier")
    TPSkillHelper.incrTPCount(player)
  }

  private def isCrossDim(player: EntityPlayer, dest: Location) = player.worldObj.provider.dimensionId != dest.dim

  object Gui {
    lazy val template = CGUIDocument.panicRead(Resources.getGui("loctele_new"))

    def dimensionNameMap(dimID: Int) = {
      DimensionManager.createProviderFor(dimID).getDimensionName
    }

    val ElemTimeStep = 0.06

    class Blend(timeOffset: Double, length: Double) {
      private val initTime = GameTimer.getTime

      def alpha: Double = {
        val dt = (GameTimer.getTime - initTime) / 1.0e3 - timeOffset
        MathUtils.clampd(0, 1, dt / length)
      }

    }

    object Colors {

      val AlphaNormal = 0.1
      val AlphaHighlight = 0.4

      val TextNormal = c(0xffc1cfd5)
      val TextHighlight = c(0xff2e3b41)
      val TextDisabled = c(0xffa2a2a2)

      private def c(hex: Int): Color = new Color(hex)
    }

    class MessageTab extends Component("MessageTab") {

      val textSize = 40
      val lineHeight = 42
      val ymargin = 20
      val xmargin = 20

      val fontOption = new FontOption(textSize, FontAlign.RIGHT, Colors.TextNormal)
      val font = Resources.font()

      var text: List[String] = Nil

      listen(classOf[FrameEvent], new IGuiEventHandler[FrameEvent] {
        override def handleEvent(w: Widget, evt: FrameEvent): Unit = {
          text.zipWithIndex.foreach { case (content, idx) =>
            val y = ymargin + lineHeight * idx
            font.draw(content, widget.transform.width - xmargin, y, fontOption)
          }
        }
      })

      def updateText(t: List[String]) = {
        text = t

        if (t.isEmpty) {
          widget.transform.doesDraw = false
        } else {
          widget.transform.doesDraw = true
          val width = text.map(font.getTextWidth(_, fontOption)).max + xmargin * 2
          val height = text.length * lineHeight + ymargin * 2

          widget.size(width, height)
        }
      }

    }

  }

  class Gui extends CGuiScreen {
    import Gui._
    import cn.lambdalib.cgui.ScalaCGUI._
    import LTNetDelegate._

    val root = template.getWidget("root").copy
    val info = root.child("info")
    val list = root.child("menu/list")

    val player = Minecraft.getMinecraft.thePlayer
    val data = LocTeleportData(player)

    var currentMessage: Option[HintMessage] = None

    { // hide templates
      val elem_template = list.child("elem_template")
      val add_template = list.child("add_template")

      elem_template.transform.doesDraw = false
      add_template.transform.doesDraw = false
    }

    { // blend in menu
      val menu = root.child("menu")
      val blend = new Blend(0, 0.4)
      val maxHeight = menu.transform.height
      menu.transform.height = 0
      menu.listens[FrameEvent](() => {
        menu.transform.height = blend.alpha * maxHeight
      })
    }

    { // Initialize info area
      info :+ new MessageTab
      info.component[MessageTab].updateText(Nil)

      info.listens[FrameEvent](() => currentMessage match {
        case Some(msg) if !msg.available =>
          setMessage(None)
        case _ => ()
      })
    }

    updateList(data.locations)

    getGui.addWidget(root)

    def wrapBack(ret: Widget, n: Int, msg: => List[String]) = {
      ret.removeComponent("Tint")

      val blend = new Blend(n * ElemTimeStep, 0.2)

      var lastHovering = false

      ret.listens[FrameEvent](() => {
        val hovering = {
          val gui = ret.getGui
          ret.isPointWithin(gui.mouseX, gui.mouseY)
        }

        if (!lastHovering && hovering) {
          lastHovering = true
          setMessage(Some(new HintMessage {
            override def available: Boolean = lastHovering
            override val ypos: Double = ret.y
            override lazy val message: List[String] = msg
          }))
        }

        val alpha0 = blend.alpha * (if (hovering) Colors.AlphaHighlight else Colors.AlphaNormal)
        Color.whiteBlend(alpha0).bind()
        HudUtils.colorRect(0, 0, ret.transform.width, ret.transform.height)

        lastHovering = hovering
      })
    }

    def wrapButton(target: Widget, n: Int, offset: Double, clickCallback: () => Any) = {
      val color = Colors.TextNormal.copy
      color.a = 0

      target.component[DrawTexture].color = color
      val blend = new Blend(n * ElemTimeStep + offset, 0.1)
      target.listens((evt: FrameEvent) => {
        val a0 = if (evt.hovering) 1.0 else 0.7
        color.a = a0 * blend.alpha
      })
      target.listens[LeftClickEvent](() => clickCallback())
    }

    private def setMessage(value: Option[HintMessage]) = {
      val (ypos, texts) = value match {
        case Some(msg) => (msg.ypos, msg.message)
        case None => (0.0, Nil)
      }

      currentMessage = value
      gui.moveWidgetToAbsPos(info, info.x, ypos)
      info.component[MessageTab].updateText(texts)
      gui.updateWidget(info)
    }

    private def updateList(locations: List[Location]): Unit = {
      list.removeComponent("ElementList")

      val compList = new ElementList
      compList.spacing = 2

      for (l <- locations) {
        compList.addWidget(newElem(l, compList.getSubWidgets.size))
      }

      compList.addWidget(newAdd(compList.size))

      list :+ compList
    }

    private def newElem(location: Location, count: Int) = {
      val ret = list.child("elem_template").copy
      ret.transform.doesDraw = true

      val textBox = ret.child("text").component[TextBox]

      textBox.setContent(location.name)

      val stat = getPerformStat(player, location)
      val (_, cp) = getConsumption(player, location)

      val message = {
        val dimensionName = dimensionNameMap(location.dim)
        val result0 = List(dimensionName + s" (#${location.dim})",
          "(%.0f, %.0f, %.0f)".format(location.x, location.y, location.z),
          "%.0f CP".format(cp))

        if (stat.isEmpty) result0 else result0 :+ stat.get
      }

      wrapBack(ret, count, message)

      if (stat.isEmpty) {
        wrapButton(ret.child("btn_teleport"), count, 0.03,
          () => {
            mc.displayGuiScreen(null)
            send(MSG_PERFORM, player, location)
          })
      } else {
        ret.removeWidget("btn_teleport")
        ret.child("text").component[TextBox].option.color.fromHexColor(0xa2a2a2)
      }

      wrapButton(ret.child("btn_remove"), count, 0.05, () => {
        LTNetDelegate.send(MSG_REMOVE, player, location.id, Future.create((list: util.List[Location]) => {
          import scala.collection.JavaConversions._
          updateList(list.toList)
        }))
      })

      {
        val wid = ret.child("text")
        val text = wid.component[TextBox]
        text.option.color.a = 0

        val blend = new Blend(count * ElemTimeStep + 0.1, 0.1)
        wid.listens[FrameEvent](() => {
          text.option.color.a = blend.alpha
        })
      }


      ret
    }

    private def newAdd(count: Int) = {
      import scala.collection.JavaConversions._

      val ret = list.child("add_template").copy

      val message = {
        val dimID = player.worldObj.provider.dimensionId
        val name = dimensionNameMap(dimID)

        List(name + s" (#$dimID)", "(%.0f, %.0f, %.0f)".format(player.posX, player.posY, player.posZ))
      }

      wrapBack(ret, count, message)

      val blend = new Blend(count * ElemTimeStep, 0.2)
      val inputText = ret.child("input_text")
      val textBox = inputText.component[TextBox]
      textBox.option.color.a = 0

      inputText.listens[FrameEvent](() => {
        textBox.option.color.a = blend.alpha * (if (inputText.isFocused) 0.8 else 0.4)
      })

      ret.listens[LeftClickEvent](() => if (!inputText.isFocused) {
        textBox.allowEdit = true
        textBox.content = ""
        getGui.gainFocus(inputText)
      })

      def confirmInput() = {
        LTNetDelegate.send(MSG_ADD, player, inputText.component[TextBox].content.take(16),
          Future.create((list: util.List[Location]) => {
            updateList(list.toList)
          }))

        gui.removeFocus()
        textBox.allowEdit = false
        textBox.content = "Add..."
      }

      inputText.listens[ConfirmInputEvent](() => confirmInput())

      wrapButton(ret.child("btn_confirm"), count, 0.0, () => confirmInput())

      ret
    }

    override def doesGuiPauseGame(): Boolean = false

    trait HintMessage {

      val ypos: Double

      val message: List[String]

      /**
        * @return Whether this message is available. Non-available message will be dropped.
        */
      def available: Boolean

    }
  }

}

object LocTeleportData {

  def apply(player: EntityPlayer) = EntityData.get(player).getPart(classOf[LocTeleportData])

}

@Registrant
@NetworkS11nType
@SerializeStrategy(strategy=ExposeStrategy.ALL)
class Location {
  var name: String = _
  var dim: Int = _
  var x: Float = _
  var y: Float = _
  var z: Float = _
  var id: Int = _

  def this(_name: String, _dim: Int, _pos: (Float, Float, Float), _id: Int) = { this
    name = _name
    dim = _dim
    _pos match { case (x2, y2, z2) =>
      x = x2
      y = y2
      z = z2
    }
    id = _id
  }
}

@Registrant
@RegDataPart(value=classOf[EntityPlayer])
class LocTeleportData extends DataPart[EntityPlayer] {

  @SerializeIncluded
  var locationList = new util.ArrayList[Location]

  setNBTStorage()
  setClientNeedSync()

  override def toNBT(tag: NBTTagCompound): Unit = NBTS11n.write(tag, this)

  override def fromNBT(tag: NBTTagCompound): Unit = NBTS11n.read(tag, this)

  def add(name: String, dim: Int, pos: (Float, Float, Float)) = {
    checkSide(Side.SERVER)
    locationList.add(new Location(name, dim, pos, locationList.size))
    sync()
  }

  def remove(id: Int): Unit = {
    import scala.collection.JavaConversions._
    checkSide(Side.SERVER)
    locationList.remove(id)

    locationList.zipWithIndex.foreach { case (a, idx) => a.id = idx }

    sync()
  }

  def locations: List[Location] = {
    import scala.collection.JavaConversions._
    locationList.toList
  }

}