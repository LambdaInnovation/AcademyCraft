package cn.academy.ability.block

import java.lang
import java.util.function.Supplier

import cn.academy.ability.api.data.CPData
import cn.academy.ability.api.data.CPData.IInterfSource
import cn.academy.core.block.ACBlockContainer
import cn.academy.core.client.Resources
import cn.academy.core.client.render.block.RenderDynamicBlock
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.{TileEntityRegistration, RegInitCallback, RegTileEntity}
import cn.lambdalib.cgui.gui.CGuiScreen
import cn.lambdalib.cgui.gui.component.DragBar.DraggedEvent
import cn.lambdalib.cgui.gui.component.TextBox.ConfirmInputEvent
import cn.lambdalib.cgui.gui.component.{TextBox, DrawTexture, DragBar}
import cn.lambdalib.cgui.gui.event.LeftClickEvent
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.networkcall.Future.FutureCallback
import cn.lambdalib.networkcall.s11n.StorageOption
import cn.lambdalib.networkcall.{Future, RegNetworkCall}
import cn.lambdalib.networkcall.s11n.StorageOption.{RangedTarget, Data}
import cn.lambdalib.util.generic.{VecUtils, MathUtils}
import cn.lambdalib.util.helper.TickScheduler
import cn.lambdalib.util.mc.{PlayerUtils, EntitySelectors, WorldUtils}
import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.{SideOnly, Side}
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{ResourceLocation, IIcon}
import net.minecraft.world.{World, IBlockAccess}

import scala.collection.JavaConversions._

@Registrant
object AbilityInterf {
  val minRange = 10.0
  val maxRange = 100.0

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
  private var range_ = minRange

  def enabled = enabled_
  def setEnabled(value: Boolean) = {
    enabled_ = value
    sync()
  }

  def range = range_
  def setRange(value: Double) = {
    range_ = value
    sync()
  }

  def placer = placer_
  def setPlacer(player: EntityPlayer) = {
    placer_ = Some(player.getCommandSenderName)
    sync()
  }

  // Check player in the area and interfere them
  scheduler.every(10).atOnly(Side.SERVER).condition(new Supplier[lang.Boolean] {
    override def get(): lang.Boolean = enabled
  }).run(new Runnable {
    override def run() = {
      val rangeVal = range
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
    }
  })

  // Sync data to client
  scheduler.every(20).atOnly(Side.SERVER).run(new Runnable {
    override def run() = {
      sync()
    }
  })

  override def updateEntity() = scheduler.runTick()

  def sync() = if (!getWorldObj.isRemote) {
    syncFromServer(this, range, placer.orNull, enabled)
  }

  @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
  private def syncFromServer(
          @RangedTarget(range = 20) me: TileAbilityInterferer,
          @Data range2 : java.lang.Double,
          @Data placer2 : String,
          @Data enabled2: java.lang.Boolean) = {
    range_ = range2
    placer_ = Option(placer2)
    enabled_ = enabled2
  }

  // Network-cross modifiers

  @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
  def syncSetEnabled(@Data state: java.lang.Boolean, @Data future: Future) = {
    setEnabled(state)
    future.setAndSync(state)
  }

  @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
  def syncSetRange(@Data range: java.lang.Double) = {
    setRange(range)
  }

  override def readFromNBT(tag: NBTTagCompound) = {
    super.readFromNBT(tag)
    enabled_ = tag.getBoolean("enabled_")
    placer_ = if(tag.hasKey("placer_")) Some(tag.getString("placer_")) else None
    range_ = tag.getFloat("range_")
  }

  override def writeToNBT(tag: NBTTagCompound) = {
    super.writeToNBT(tag)
    tag.setBoolean("enabled_", enabled_)
    placer_ match {
      case Some(name) => tag.setString("placer_", name)
      case _ =>
    }
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

  @SideOnly(Side.CLIENT)
  override def onBlockActivated(world: World, x: Int, y: Int, z: Int,
                                player: EntityPlayer, side: Int,
                                tx: Float, ty: Float, tz: Float) =
    if (world.isRemote) {
      world.getTileEntity(x, y, z) match {
        case tile: TileAbilityInterferer =>
          // Client side verification might be dangerous, but its really OK in here.
          if (player.capabilities.isCreativeMode ||
            Option(player.getCommandSenderName) == tile.placer) {
            Minecraft.getMinecraft.displayGuiScreen(new GuiAbilityInterferer(tile))
          } else {
            PlayerUtils.sendChat(player, "ac.ability_interf.cantuse")
          }
          true
        case _ => false
      }
    } else false

}

@SideOnly(Side.CLIENT)
object GuiAbilityInterferer {
  val tex_switchOn = Resources.getTexture("guis/button/button_switchon")
  val tex_switchOff = Resources.getTexture("guis/button/button_switchoff")
}

class GuiAbilityInterferer(tile: TileAbilityInterferer) extends CGuiScreen {
  import cn.lambdalib.cgui.ScalaCGUI._
  import AbilityInterf._
  import GuiAbilityInterferer._

  val main = CGUIDocument.panicRead(new ResourceLocation("academy:guis/ability_interf.xml")).getWidget("window_main")
  val switch    = main.getWidget("btn_switch")
  val bar       = main.getWidget("btn_point")
  val textRange = main.getWidget("text_range")

  updateRange(tile.range)
  updateState(tile.enabled)

  switch.listens[LeftClickEvent](() => {
    tile.syncSetEnabled(!tile.enabled, Future.create(new FutureCallback[Boolean] {
      override def onReady(value: Boolean) = {
        updateState(value)
      }
    }))
  })

  bar.listens[DraggedEvent](() => {
    TextBox.get(textRange).setContent("%.1f".format(MathUtils.lerp(minRange, maxRange, DragBar.get(bar).getProgress)))
  })

  main.getWidget("btn_confirm").listens[LeftClickEvent](() => {
    syncRange_()
    mc.displayGuiScreen(null)
  })

  textRange.listens((w, evt: ConfirmInputEvent) => {
    val textBox = TextBox.get(w)
    try {
      val dragbar = DragBar.get(bar)
      val input = MathUtils.clampd(minRange, maxRange, textBox.content.toDouble)
      updateRange(input)
      syncRange_()
    } catch {
      case e: NumberFormatException =>
        textBox.setContent(tile.range.toString)
    }
  })

  gui.addWidget("main", main)

  private def updateState(state: Boolean) = {
    DrawTexture.get(switch).setTex(if (tile.enabled) tex_switchOn else tex_switchOff)
  }

  private def updateRange(input: Double) = {
    DragBar.get(bar).setProgress((input - minRange) / (maxRange - minRange))
    TextBox.get(textRange).setContent("%.1f".format(input))
  }

  private def syncRange_() = tile.syncSetRange(MathUtils.lerp(minRange, maxRange, DragBar.get(bar).getProgress))

  override def doesGuiPauseGame = false

}
