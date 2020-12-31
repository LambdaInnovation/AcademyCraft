package cn.academy.medicine

import cn.academy.Resources
import cn.academy.block.block.ACBlockContainer
import cn.academy.block.container.TechUIContainer
import cn.academy.block.tileentity.TileReceiverBase
import cn.academy.container.{SlotConditional, SlotOutput}
import cn.lambdalib2.cgui.component.ProgressBar
import cn.lambdalib2.cgui.event.{FrameEvent, LeftClickEvent}
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.registry.mc.RegTileEntity
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.{NetworkMessage, NetworkS11n, TargetPoints}
import cn.lambdalib2.util.{RandUtils, TickScheduler}
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import java.util.function.Predicate

//import cn.academy.core.Resources
//import cn.academy.core.block.{ACBlockContainer, TileReceiverBase}
import cn.academy.core.client.ui.TechUI.ContainerUI
import cn.academy.core.client.ui.{InventoryPage, WirelessPage}
//import cn.academy.core.container.{SlotConditional, SlotOutput, TechUIContainer}
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.academy.medicine.Properties._
//import cn.lambdalib.annoreg.core.Registrant
//import cn.lambdalib.annoreg.mc.{RegInitCallback, RegTileEntity}
//import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase
//import cn.lambdalib.cgui.gui.component.ProgressBar
//import cn.lambdalib.cgui.gui.event.{FrameEvent, LeftClickEvent}
//import cn.lambdalib.cgui.xml.CGUIDocument
//import cn.lambdalib.s11n.network.NetworkMessage.Listener
//import cn.lambdalib.s11n.network.{NetworkMessage, NetworkS11n, TargetPoints}
//import cn.lambdalib.template.container.CleanContainer
//import cn.lambdalib.util.generic.RandUtils
//import cn.lambdalib.util.helper.TickScheduler
//import cn.lambdalib.util.mc.SideHelper
//import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{IInventory, Slot}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.reflect.ClassTag

object MedSynthesizer {

  def inputSlot(id: Int) = {
    require(id >= 0 && id < 4)
    id
  }

  def isInputSlot(slotID: Int) = slotID >= 0 && slotID < 4

  val bottleSlot = 4

  val outputSlot = 5

  val guiHandler = new GuiHandlerBase {
    override protected def getServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
      world.getTileEntity(new BlockPos(x, y, z)) match {
        case tile: TileMedSynthesizer => new ContainerMedSynthesizer(player, tile)
        case _ => null
      }
    }

    @SideOnly(Side.CLIENT)
    override protected def getClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
      Option(getServerContainer(player, world, x, y, z).asInstanceOf[ContainerMedSynthesizer])
        .map(GuiMedSynthesizer.apply).orNull
    }
  }

  val ProgPerTick = 0.04f
  val ConsumePerSynth = 2000
  val ConsumePerTick: Double = ConsumePerSynth * ProgPerTick

  def synth(mats: List[Property]): MedicineApplyInfo = {
    synthDirect(mats) match {
      case Some(info) => info
      case _ =>
        MedicineApplyInfo(Properties.Targ_Disposed, Properties.Str_Strong, 1.5f, Properties.Apply_Instant_Decr,
          Properties.Targ_Disposed.medSensitiveRatio * RandUtils.rangef(0.8f, 1.2f))
    }
  }

  def synthDirect(mats: List[Property]): Option[MedicineApplyInfo] = {
    def findOne[T <: Property](implicit tag: ClassTag[T]) = {
      val list2 = mats.filter(tag.runtimeClass.isInstance)
      if (list2.size == 1) Some(list2.head.asInstanceOf[T]) else None
    }

    val targets = findOne[Target]
    val strengths = findOne[Strength]
    val methods = findOne[ApplyMethod]
    val vars = findOne[Variation]

    (targets, strengths, methods) match {
      case (Some(targ), Some(str), Some(method)) =>
        var variationRange = (0.8f, 1.2f)
        var strValue = str.baseValue * method.strength
        var medSensValue = targ.medSensitiveRatio
        var rStrength = str
        var rMethod = method

        def randomRatio = RandUtils.rangef(variationRange._1, variationRange._2)

        // Process variation
        vars match {
          case Some(Properties.Var_Infinity) =>
            strValue = 10000
            medSensValue = 0.99f
            rStrength = Properties.Str_Infinity

          case Some(Properties.Var_Desens) =>
            medSensValue = -1.2f * medSensValue

            val incr = RandUtils.nextFloat() < 0.05f
            rMethod = Properties.findApplyMethod(method.instant, incr)

          case Some(Properties.Var_Fluct) =>
            variationRange = (0.6f, 1.5f)

          case Some(Properties.Var_Stabilize) =>
            variationRange = (0.9f, 1.1f)

          case Some(Properties.Var_Neutralize) =>
            // TODO

          case _ =>
        }

        Some(MedicineApplyInfo(targ, rStrength, strValue * randomRatio, rMethod, medSensValue * randomRatio))
      case _ => None
    }
  }


}

import MedSynthesizer._

object BLockMedSynthesizer extends ACBlockContainer(net.minecraft.block.material.Material.ROCK, guiHandler) {

  override def createNewTileEntity(world: World, meta: Int): TileEntity = new TileMedSynthesizer

}

@RegTileEntity
class TileMedSynthesizer extends TileReceiverBase("medicine_synthesizer", 6, 10000, 100) {
  import scala.collection.JavaConversions._

  private var progress_ = 0.0f
  private var synthesizing_ = false

  val scheduler = new TickScheduler
  scheduler.every(5).atOnly(Side.SERVER).run(() => sync())

  override def updateEntity() = {
    val world = getWorld

    if (synthesizing_) {
      progress_ = math.min(1, progress_ + ProgPerTick)
      val consEnergy = pullEnergy(ConsumePerTick) == ConsumePerTick

      if (!world.isRemote) {
        def endSynth() = {
          synthesizing_ = false
          progress_ = 0.0f
        }

        if (progress_ == 1.0f) {
          doSynth()
          endSynth()
        } else if (!consEnergy || !canSynth) {
          endSynth()
        }

      }
    }

    super.updateEntity()
    scheduler.runTick()
  }

  def beginSynth() = {
    require(!getWorld.isRemote)
    if (!synthesizing_ && canSynth) {
      progress_ = 0.0f
      synthesizing_ = true
      sync()
    }
  }

  private def sync() = {
    NetworkMessage.sendToAllAround(TargetPoints.convert(this, 8), this, "synth_sync",
      synthesizing_.asInstanceOf[AnyRef], progress_.asInstanceOf[AnyRef])
  }

  private def canSynth = {
    inventory(bottleSlot) != null && inventory.take(4).exists(_ != null)
  }

  private def doSynth(): Unit = {
    require(!getWorld.isRemote)

    val result = synth(inventory.take(4).toList.filter(_ != null).map(ItemPowder.getProperty))
    val resultStack = ItemMedicineBottle.create(result)

    (0 until 4).map(inputSlot).foreach(inventory.update(_, null))
    setInventorySlotContents(outputSlot, resultStack)

    inventory(bottleSlot).getCount -= 1
    if (inventory(bottleSlot).getCount == 0) {
      inventory.update(bottleSlot, null)
    }
  }

  def synthProgress = progress_
  def synthesizing = synthesizing_

  @Listener(channel="synth_sync", side=Array(Side.CLIENT))
  private def hSyncSynth(ss: Boolean, pr: Float) = {
    synthesizing_ = ss
    progress_ = pr
  }

}

class ContainerMedSynthesizer(p: EntityPlayer, t: TileMedSynthesizer) extends
  TechUIContainer[TileMedSynthesizer](p, t) {
  import cn.lambdalib2.template.container.CleanContainer._

  class SlotPowder(inv: IInventory, slot: Int, x: Int, y: Int) extends Slot(inv, slot, x, y) {
    override def isItemValid(stack : ItemStack): Boolean = stack.getItem.isInstanceOf[ItemPowder]
  }

  def slotPowder(id: Int, x: Int, y: Int) =
    SlotConditional.apply(stack => stack.getItem.isInstanceOf[ItemPowder], tile, inputSlot(id), x, y)

  addSlotToContainer(slotPowder(0, 34, 12))
  addSlotToContainer(slotPowder(1, 10, 32))
  addSlotToContainer(slotPowder(2, 10, 58))
  addSlotToContainer(slotPowder(3, 35, 78))

  addSlotToContainer(SlotConditional.apply(_.getItem == ModuleMedicine.emptyBottle, tile, bottleSlot, 50, 45))
  addSlotToContainer(new SlotOutput(tile, outputSlot, 138, 44))

  val gInv = gRange(6, 6 + 36)
  val gPowders = gRange(0, 4)
  val gBottle = gSlots(4)
  val gOutput = gSlots(5)

  addTransferRule(gInv, p(stack => stack.getItem.isInstanceOf[ItemPowder]), gPowders)
  addTransferRule(gInv, p(stack => stack.getItem == ModuleMedicine.emptyBottle), gBottle)
  addTransferRule(gRange(0, 6), gInv)

  private def p(fn: ItemStack=>Boolean) = new Predicate[ItemStack] {
    override def test(t: ItemStack): Boolean = fn(t)
  }

  mapPlayerInventory()

}

private object MSNetEvents {

  @RegInitCallback
  def init() = {
    NetworkS11n.addDirectInstance(MSNetEvents)
  }

  final val MSG_BEGIN_SYNTH = "begin"

  @Listener(channel=MSG_BEGIN_SYNTH, side=Array(Side.SERVER))
  def hBegin(tile: TileMedSynthesizer) = {
    tile.beginSynth()
  }

}

object GuiMedSynthesizer {
  import cn.lambdalib2.cgui.ScalaCGUI._
  import MSNetEvents.MSG_BEGIN_SYNTH

  private lazy val template = CGUIDocument.read(Resources.getGui("rework/page_med_synth")).getWidget("main")

  def apply(container: ContainerMedSynthesizer) = {
    val tile = container.tile

    val invWidget = template.copy

    val invPage = InventoryPage(invWidget)
    val wirelessPage = WirelessPage.userPage(tile)

    invWidget.child("btn_go").listens[LeftClickEvent](() => {
      NetworkMessage.sendToServer(MSNetEvents, MSG_BEGIN_SYNTH, tile)
    })

    {
      val widget = invWidget.child("progress")
      val progress = widget.component[ProgressBar]
      widget.listens[FrameEvent](() => {
        progress.progress = tile.synthProgress
      })
    }

    val ret = new ContainerUI(container, invPage, wirelessPage)

    ret
  }

}
