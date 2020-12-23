package cn.academy.ability.vanilla.teleporter.skill

import java.util
import java.util.function.{Consumer, Predicate}

import cn.academy.Resources
import cn.academy.ability.{AbilityContext, Skill}
import cn.academy.ability.context.{ClientRuntime, KeyDelegate}
import cn.academy.client.sound.ACSounds
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper
import cn.academy.advancements.ACAdvancements
import cn.academy.datapart.AbilityData
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent
import cn.lambdalib2.cgui.{CGuiScreen, Widget}
import cn.lambdalib2.cgui.component._
import cn.lambdalib2.cgui.event.{FrameEvent, IGuiEventHandler, LeftClickEvent}
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.datapart.{DataPart, EntityData, RegDataPart}
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.render.font.IFont.{FontAlign, FontOption}
import cn.lambdalib2.s11n.{SerializeIncluded, SerializeStrategy}
import cn.lambdalib2.s11n.SerializeStrategy.ExposeStrategy
import cn.lambdalib2.s11n.nbt.NBTS11n
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, NetworkS11n, NetworkS11nType}
import cn.lambdalib2.util._
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{ResourceLocation, SoundCategory}
import net.minecraft.util.math.MathHelper
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.util.Color

private object LTNetDelegate {
  final val MSG_ADD = "add"
  final val MSG_REMOVE = "remove"
  final val MSG_QUERY = "query"
  final val MSG_PERFORM = "perform"
  final val MSG_SOUND = "playsound"

  import scala.collection.JavaConversions._
  import LocationTeleport._

  @StateEventCallback
  def _init(fMLInitializationEvent: FMLInitializationEvent) = {
    NetworkS11n.register(classOf[util.ArrayList[_]])
    NetworkS11n.addDirectInstance(LTNetDelegate)
  }

  def send(channel: String, args: Any*) = {
    NetworkMessage.sendToServer(LTNetDelegate, channel, args.map(_.asInstanceOf[AnyRef]): _*)
  }

  @Listener(channel=MSG_ADD, side=Array(Side.SERVER))
  private def hAdd(player: EntityPlayer, name: String, future: Future[util.List[Location]]) = {
    val data = LocTeleportData(player)
    data.add(name, player.world.provider.getDimension,
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

  @SideOnly(Side.CLIENT)
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
      math.max(8.0f, MathHelper.sqrt(math.min(800, distance))))
  }

  /**
    * @return `None` if can perform. `Some(reason)` if can't.
    */
  def getPerformStat(player: EntityPlayer, dest: Location): Option[String] = {
    def fail(id: String) = Some(I18n.format("ac.gui.loctele." + id))

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
      entitiesToTeleport.foreach(_.changeDimension(dest.dim))
    }

    val dist = player.getDistance(dest.x, dest.y, dest.z)
    val expincr = if (dist >= 200) 0.03f else 0.015f
    val (px, py, pz) = (player.posX, player.posY, player.posZ)
    entitiesToTeleport.foreach(e => {
      val (dx, dy, dz) = (e.posX - px, e.posY - py, e.posZ - pz)
      if(e.isRiding)e.dismountRidingEntity()
      e.setPositionAndUpdate(dest.x + dx, dest.y + dy, dest.z + dz)
    })

    ctx.addSkillExp(expincr)
    ctx.setCooldown(MathUtils.lerpf(30, 20, ctx.getSkillExp).toInt)

    TPSkillHelper.incrTPCount(player)
  }

  private def isCrossDim(player: EntityPlayer, dest: Location) = player.world.provider.getDimension != dest.dim

  object Gui {
    lazy val template = CGUIDocument.read(Resources.getGui("loctele_new"))

    def dimensionNameMap(dimID: Int) = {
      DimensionManager.createProviderFor(dimID).getDimension
    }

    val ElemTimeStep = 0.06

    class Blend(timeOffset: Double, length: Double) {
      private val initTime = GameTimer.getTime

      def alpha: Float = {
        val dt = (GameTimer.getTime - initTime) / 1.0 - timeOffset
        MathUtils.clampd(0, 1, dt / length).toFloat
      }

    }

    object DefColors {

      val AlphaNormal = 0.1f
      val AlphaHighlight = 0.4f

      val TextNormal = c(0xffc1cfd5)
      val TextHighlight = c(0xff2e3b41)
      val TextDisabled = c(0xffa2a2a2)

      private def c(hex: Int): Color = {
        val a = (hex & 0xff000000)>>24
        val r = (hex & 0x00ff0000)>>16
        val g = (hex & 0x0000ff00)>>8
        val b = (hex & 0x000000ff)>>0
        new Color(r,g,b,a)

      }
    }

    class MessageTab extends Component("MessageTab") {

      val textSize = 40
      val lineHeight = 42
      val ymargin = 20
      val xmargin = 20

      val fontOption = new FontOption(textSize, FontAlign.RIGHT, DefColors.TextNormal)
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
    import LTNetDelegate._
    import cn.lambdalib2.cgui.ScalaCGUI._

    val root = template.getWidget("root").copy
    val info = root.getWidget("info")
    val list = root.getWidget("menu/list")

    val player = Minecraft.getMinecraft.player
    val data = LocTeleportData(player)

    var currentMessage: Option[HintMessage] = None

    { // hide templates
      val elem_template = list.getWidget("elem_template")
      val add_template = list.getWidget("add_template")

      elem_template.transform.doesDraw = false
      add_template.transform.doesDraw = false
    }

    { // blend in menu
      val menu = root.getWidget("menu")
      val blend = new Blend(0, 0.4)
      val maxHeight = menu.transform.height
      menu.transform.height = 0
      menu.listen(classOf[FrameEvent], new Runnable {
        override def run(): Unit = {
          menu.transform.height = (blend.alpha * maxHeight).toFloat
        }
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
      ret.removeComponent(classOf[Tint])

      val blend = new Blend(n * ElemTimeStep, 0.2)

      var lastHovering = false

      ret.listens[FrameEvent](() => {
        val hovering = {
          val gui = ret.getGui
          ret.isPointWithin(gui.getMouseX, gui.getMouseY)
        }

        if (!lastHovering && hovering) {
          lastHovering = true
          setMessage(Some(new HintMessage {
            override def available: Boolean = lastHovering
            override val ypos: Double = ret.y
            override lazy val message: List[String] = msg
          }))
        }

        val alpha0 = blend.alpha * (if (hovering) DefColors.AlphaHighlight else DefColors.AlphaNormal)
        Colors.bindToGL(Colors.whiteBlend(alpha0))//TODO need support
        HudUtils.colorRect(0, 0, ret.transform.width, ret.transform.height)

        lastHovering = hovering
      })
    }

    def wrapButton(target: Widget, n: Int, offset: Double, clickCallback: () => Any) = {
      val color = new Color(DefColors.TextNormal)
      color.setAlpha(0)

      target.component[DrawTexture].color = color
      val blend = new Blend(n * ElemTimeStep + offset, 0.1)
      target.listens((evt: FrameEvent) => {
        val a0 = if (evt.hovering) 1.0 else 0.7
        color.setAlpha((a0 * blend.alpha*255).toInt)
      })
      target.listens[LeftClickEvent](() => clickCallback())
    }

    private def setMessage(value: Option[HintMessage]) = {
      val (ypos, texts) = value match {
        case Some(msg) => (msg.ypos, msg.message)
        case None => (0.0, Nil)
      }

      currentMessage = value
      gui.moveWidgetToAbsPos(info, info.x, ypos.toFloat)
      info.component[MessageTab].updateText(texts)
      gui.updateWidget(info)
    }

    private def updateList(locations: List[Location]): Unit = {
      list.removeComponent(classOf[ElementList])

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

            ACSounds.playClient(player, "tp.tp", SoundCategory.AMBIENT, .5f)
            send(MSG_PERFORM, player, location)
          })
      } else {
        ret.removeWidget("btn_teleport")
        ret.child("text").component[TextBox].option.color.set(0xa2, 0xa2, 0xa2)
      }

      wrapButton(ret.child("btn_remove"), count, 0.05, () => {
        LTNetDelegate.send(MSG_REMOVE, player, location.id, Future.create(new Consumer[util.List[Location]]{
          override def accept(list: util.List[Location]) = {
          import scala.collection.JavaConversions._
          updateList(list.toList)
        }}))
      })

      {
        val wid = ret.child("text")
        val text = wid.component[TextBox]
        text.option.color.setAlpha((0*255).toInt)

        val blend = new Blend(count * ElemTimeStep + 0.1, 0.1)
        wid.listens[FrameEvent](() => {
          text.option.color.setAlpha((blend.alpha*255).toInt)
        })
      }


      ret
    }

    private def newAdd(count: Int) = {
      import scala.collection.JavaConversions._

      val ret = list.child("add_template").copy

      val message = {
        val dimID = player.getEntityWorld.provider.getDimension
        val name = dimensionNameMap(dimID)

        List(name + s" (#$dimID)", "(%.0f, %.0f, %.0f)".format(player.posX, player.posY, player.posZ))
      }

      wrapBack(ret, count, message)

      val blend = new Blend(count * ElemTimeStep, 0.2)
      val inputText = ret.child("input_text")
      val textBox = inputText.component[TextBox]
      textBox.option.color.setAlpha((0*255).toInt)

      inputText.listens[FrameEvent](() => {
        textBox.option.color.setAlpha((blend.alpha * (if (inputText.isFocused) 0.8 else 0.4)*255).toInt)
      })

      ret.listens[LeftClickEvent](() => if (!inputText.isFocused) {
        textBox.allowEdit = true
        textBox.content = ""
        getGui.gainFocus(inputText)
      })

      def confirmInput() = {
        LTNetDelegate.send(MSG_ADD, player, inputText.component[TextBox].content.take(16),
          Future.create(new Consumer[util.List[Location]]{
            override def accept(list: util.List[Location]): Unit ={
            updateList(list.toList)
          }}))

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