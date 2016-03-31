/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.block

import java.lang
import java.util.function.Supplier

import cn.academy.ability.api.data.CPData
import cn.academy.ability.api.data.CPData.IInterfSource
import cn.academy.core.block.ACBlockContainer
import cn.academy.core.client.Resources
import cn.academy.core.client.render.block.RenderDynamicBlock
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.{RegInitCallback, RegTileEntity}
import cn.lambdalib.cgui.gui.component.TextBox.ConfirmInputEvent
import cn.lambdalib.cgui.gui.component.{Component, DrawTexture, ElementList, TextBox}
import cn.lambdalib.cgui.gui.event.{FrameEvent, GainFocusEvent, LeftClickEvent, LostFocusEvent}
import cn.lambdalib.cgui.gui.{CGuiScreen, HierarchyDebugger, Widget}
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.networkcall.TargetPointHelper
import cn.lambdalib.s11n.nbt.NBTS11n
import cn.lambdalib.s11n.network.{Future, NetworkMessage}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.generic.{MathUtils, RandUtils, VecUtils}
import cn.lambdalib.util.helper.TickScheduler
import cn.lambdalib.util.mc.{EntitySelectors, PlayerUtils, WorldUtils}
import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{IIcon, ResourceLocation}
import net.minecraft.world.{IBlockAccess, World}

import scala.collection.JavaConversions._
import scala.collection.SortedSet

@Registrant
object AbilityInterf {
  val minRange = 10.0
  val maxRange = 100.0

  final val MSG_SYNC = "sync"
  final val MSG_UPDATE_RANGE = "set_range"
  final val MSG_UPDATE_WHITELIST = "set_whitelist"
  final val MSG_UPDATE_ENABLED = "set_enabled"

  @SideOnly(Side.CLIENT)
  @RegInitCallback
  def regClient() = ClientRegistry.bindTileEntitySpecialRenderer(classOf[TileAbilityInterferer], new RenderDynamicBlock)
}

@Registrant
@RegTileEntity
class TileAbilityInterferer extends TileEntity {
  import AbilityInterf._

  val scheduler = new TickScheduler

  lazy val sourceName = s"interferer@${getWorldObj.provider.dimensionId}($xCoord,$yCoord,$zCoord)"
  def testBB = WorldUtils.minimumBounds(
    VecUtils.vec(xCoord + 0.5 - range_, yCoord + 0.5 - range_, zCoord + 0.5 - range_),
    VecUtils.vec(xCoord + 0.5 + range_, yCoord + 0.5 + range_, zCoord + 0.5 + range_))

  private var enabled_ = false
  private var placer_ : Option[String] = None
  private var whitelist_ = SortedSet[String]()
  private var range_ = minRange

  def enabled = enabled_

  def range = range_

  def whitelist = whitelist_

  def placer = placer_

  def setPlacer(p: EntityPlayer) = if (placer_.isEmpty) {
    whitelist_ = whitelist + p.getCommandSenderName
    placer_ = Some(p.getCommandSenderName)
  }

  private def send(channel: String, args: Any*) = {
    val args2 = args.map(_.asInstanceOf[AnyRef])
    if (getWorldObj.isRemote) {
      NetworkMessage.sendToServer(this, channel, args2: _*)
    } else {
      NetworkMessage.sendToAllAround(TargetPointHelper.convert(this, 15), this, channel, args2: _*)
    }
  }

  private def sync() = {
    assert(!getWorldObj.isRemote)

    send(MSG_SYNC, range, enabled, whitelist.toArray)
  }

  // Check player in the area and interfere them
  scheduler.every(10).atOnly(Side.SERVER).condition(new Supplier[lang.Boolean] {
    override def get(): lang.Boolean = enabled
  }).run(() => {
    val boundingBox = testBB
    val players = WorldUtils.getEntities(getWorldObj, boundingBox, EntitySelectors.survivalPlayer)
    players foreach {
      case player: EntityPlayer =>
        CPData.get(player).addInterf(sourceName, new IInterfSource {
          override def interfering(): Boolean =
            boundingBox.isVecInside(VecUtils.vec(player.posX, player.posY, player.posZ)) &&
              !TileAbilityInterferer.this.isInvalid &&
              !player.capabilities.isCreativeMode &&
              enabled
        })
    }
  })

  // Sync data to client
  scheduler.every(20).atOnly(Side.SERVER).run(() => sync())

  override def updateEntity() = scheduler.runTick()

  @Listener(channel=MSG_SYNC, side=Array(Side.CLIENT))
  private def hSync(range2: Double, enabled2: Boolean, whitelist2 : Array[String]) = {
    range_ = range2
    enabled_ = enabled2
    whitelist_ = SortedSet(whitelist2: _*)
  }

  // Network-cross modifiers
  private def signalFuture(cb: () => Any) = Future.create((_: Any) => cb())

  def setRangeClient(value: Double, callback: () => Any) = send(MSG_UPDATE_RANGE, value, signalFuture(callback))

  def setEnabledClient(value: Boolean, callback: () => Any) = send(MSG_UPDATE_ENABLED, value, signalFuture(callback))

  def setWhitelistClient(value: Iterable[String], callback: () => Any) = send(MSG_UPDATE_WHITELIST, value.toArray, signalFuture(callback))

  @Listener(channel=MSG_UPDATE_RANGE, side=Array(Side.SERVER))
  private def hSetRange(value: Double, fut: Future[Boolean]) = {
    range_ = MathUtils.clampd(minRange, maxRange, value)
    fut.sendResult(true)
  }

  @Listener(channel=MSG_UPDATE_ENABLED, side=Array(Side.SERVER))
  private def hSetEnabled(value: Boolean, fut: Future[Boolean]) = {
    enabled_ = value
    fut.sendResult(true)
  }

  @Listener(channel=MSG_UPDATE_WHITELIST, side=Array(Side.SERVER))
  private def hSetWhitelist(value: Array[String], fut: Future[Boolean]) = {
    whitelist_ = SortedSet(value: _*)
    fut.sendResult(true)
  }

  override def readFromNBT(tag: NBTTagCompound) = {
    super.readFromNBT(tag)
    enabled_ = tag.getBoolean("enabled_")
    whitelist_ = SortedSet(NBTS11n.readBase(tag.getTag("whitelist_"), classOf[Array[String]]): _*)
    range_ = tag.getFloat("range_")
  }

  override def writeToNBT(tag: NBTTagCompound) = {
    super.writeToNBT(tag)
    tag.setBoolean("enabled_", enabled_)
    tag.setTag("whitelist_", NBTS11n.writeBase(whitelist.toArray))
    tag.setFloat("range_", range_.toFloat)
  }

}

class AbilityInterferer extends ACBlockContainer("ability_interferer", Material.rock) {

  var iconOn: IIcon = null
  var iconOff: IIcon = null

  override def createNewTileEntity(world: World, meta: Int) = new TileAbilityInterferer

  override def registerBlockIcons(ir: IIconRegister) = {
    iconOn = ricon(ir, "ability_interf_on")
    iconOff = ricon(ir, "ability_interf_off")
  }

  override def onBlockPlacedBy(world : World, x : Int, y : Int, z : Int,
                               placer : EntityLivingBase, stack : ItemStack) =
    (placer, world.getTileEntity(x, y, z)) match {
      case (player: EntityPlayer, interf: TileAbilityInterferer) =>
        interf.setPlacer(player)
      case _ =>
    }


  override def getIcon(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = {
    world.getTileEntity(x, y, z) match {
      case tile: TileAbilityInterferer => if (tile.enabled) iconOn else iconOff
      case _ => iconOn
    }
  }

  override def getIcon(side: Int, meta: Int) = iconOff

  @SideOnly(Side.CLIENT)
  override def getRenderBlockPass = -1

  override def isOpaqueCube = false

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int,
                                player: EntityPlayer, side: Int,
                                tx: Float, ty: Float, tz: Float) =
    world.getTileEntity(x, y, z) match {
      case tile: TileAbilityInterferer =>
        // Client side verification might be dangerous, but its really OK in here.
        if (world.isRemote) {
          handleClient(player, tile)
        }
        true
      case _ => false
    }

  @SideOnly(Side.CLIENT)
  private def handleClient(player: EntityPlayer, tile: TileAbilityInterferer) = {
    if (player.capabilities.isCreativeMode ||
      Option(player.getCommandSenderName) == tile.placer) {
      Minecraft.getMinecraft.displayGuiScreen(GuiAbilityInterferer(tile))
    } else {
      PlayerUtils.sendChat(player, "ac.ability_interf.cantuse")
    }
  }

}

@SideOnly(Side.CLIENT)
object GuiAbilityInterferer {

  import cn.lambdalib.cgui.ScalaCGUI._
  import cn.academy.core.client.ui._
  import AbilityInterf._

  lazy val template = CGUIDocument.panicRead(Resources.getGui("rework/page_interfere")).getWidget("main")

  val buttonOn  = Resources.getTexture("guis/button/button_switch_on")
  val buttonOff = Resources.getTexture("guis/button/button_switch_off")

  def apply(tile: TileAbilityInterferer) = {
    val window = template.copy()

    {
      case class Element(playerName: String) extends Component("Element")
      class Area(var focus: Option[Widget]) extends Component("Area")

      val listPanel = window.child("panel_whitelist")
      val listArea = listPanel.child("zone_whitelist")

      val element = listArea.child("element")
      val area = new Area(None)

      listArea.removeWidget("element")
      listArea :+ area

      def update(whitelist: Iterable[String]) = {
        listArea.removeComponent("ElementList")
        area.focus = None

        val elist = new ElementList

        whitelist.foreach(name => {
          val instance = element.copy()
          val dt = instance.component[DrawTexture]
          dt.color.a = 0.7

          instance.child("element_name").component[TextBox].content = name
          instance.listens[FrameEvent](() => dt.color.a = area.focus match {
            case Some(f) if f == instance => 1.0
            case _ => 0.7
          })
          instance.listens[LeftClickEvent](() => area.focus = Some(instance))
          instance :+ new Element(name)

          elist.addWidget(instance)
        })

        listArea :+ elist
      }

      def sendUpdate(whitelist: Iterable[String]) = {
        tile.setWhitelistClient(whitelist, () => update(whitelist))
      }

      listPanel.child("btn_up").listens[LeftClickEvent](() => listArea.component[ElementList].progressLast())
      listPanel.child("btn_down").listens[LeftClickEvent](() => listArea.component[ElementList].progressNext())
      listPanel.child("btn_add").listens[LeftClickEvent](() => {
        val box = new Widget().size(40, 10).pos(50, 5)
          .addComponent(new DrawTexture(null).setColor4i(255, 255, 255, 50))
          .addComponent(Resources.newTextBox().allowEdit())

        box.listens[ConfirmInputEvent](() => {
          box.component[TextBox].content match {
            case "" =>
            case str => sendUpdate(tile.whitelist + str)
          }
          box.dispose()
        })
        box.listens[LostFocusEvent](() => box.dispose())
        listPanel :+ box
        box.gainFocus()
      })
      listPanel.child("btn_remove").listens((w, e: LeftClickEvent) => {
        listArea.component[Area].focus match {
          case Some(widget) => {
            val name = widget.component[Element].playerName
            sendUpdate(tile.whitelist - name)
          }
          case None =>
        }
      })

      update(tile.whitelist)
    }

    {
      val button = window.child("panel_config/element_switch/element_btn_switch")
      val texture = button.component[DrawTexture]
      val color = texture.color
      var state = tile.enabled

      def setState(state2: Boolean) = {
        state = state2

        val lum = if (state) 1 else 0.6
        color.r = lum
        color.g = lum
        color.b = lum

        texture.texture = if (state) buttonOn else buttonOff
      }

      setState(state)

      button.listens[LeftClickEvent](() => {
        tile.setEnabledClient(!state, () => setState(!state))
      })
    }

    {
      val elemRange = window.child("panel_config/element_range")

      def updateRange(value: Double) = elemRange.child("element_text_range").component[TextBox].content = value.toString
      def handle(delta: Int) = () => {
        val newValue = MathUtils.clampd(minRange, maxRange, tile.range + delta)
        tile.setRangeClient(newValue, () => updateRange(newValue))
      }

      updateRange(tile.range)

      elemRange.child("element_btn_left").listens[LeftClickEvent](handle(-10))
      elemRange.child("element_btn_right").listens[LeftClickEvent](handle(10))
    }

    val ret = new CGuiScreen() {
      override def doesGuiPauseGame = false
    }

    val invPage = InventoryPage(window)

    val root = TechUI(invPage)
    ret.gui.addWidget(root)
    
    ret
  }

}
