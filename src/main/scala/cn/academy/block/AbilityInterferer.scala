package cn.academy.block

import java.lang
import java.util.function.{Consumer, Supplier}

import cn.academy.client.render.block.RenderDynamicBlock
import cn.academy.energy.IFConstants
import cn.academy.support.EnergyItemHelper
import cn.academy.block.tileentity.TileReceiverBase
import cn.academy.datapart.CPData
import cn.academy.datapart.CPData.IInterfSource
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.registry.mc.RegTileEntity
import cn.lambdalib2.s11n.nbt.NBTS11n
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, TargetPoints}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.{MathUtils, VecUtils}
import cn.lambdalib2.util.TickScheduler
import cn.lambdalib2.util.{EntitySelectors, WorldUtils}
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.common.event.FMLInitializationEvent

import scala.collection.JavaConversions._
import scala.collection.SortedSet

object AbilityInterf {
  val minRange = 10.0
  val maxRange = 100.0

  final val MSG_SYNC = "sync"
  final val MSG_UPDATE_RANGE = "set_range"
  final val MSG_UPDATE_WHITELIST = "set_whitelist"
  final val MSG_UPDATE_ENABLED = "set_enabled"
  val SLOT_BATTERY = 0

  @SideOnly(Side.CLIENT)
  @StateEventCallback
  def regClient(ev: FMLInitializationEvent) = ClientRegistry.bindTileEntitySpecialRenderer(classOf[TileAbilityInterferer], (new RenderDynamicBlock).asInstanceOf[TileEntitySpecialRenderer[TileAbilityInterferer]])
}

@RegTileEntity
class TileAbilityInterferer extends TileReceiverBase("ability_interferer",1,10000, IFConstants.LATENCY_MK1) with ISidedInventory {
  import AbilityInterf._

  val scheduler = new TickScheduler

  lazy val sourceName = s"interferer@${getWorld.provider.getDimension} $pos"
  def testBB = WorldUtils.minimumBounds(
    new Vec3d(pos.getX + 0.5 - range_, pos.getY + 0.5 - range_, pos.getZ + 0.5 - range_),
    new Vec3d(pos.getX + 0.5 + range_, pos.getY + 0.5 + range_, pos.getZ + 0.5 + range_))

  private var enabled_ = false
  private var placer_ : Option[String] = None
  private var whitelist_ = SortedSet[String]()
  private var range_ = minRange

  def enabled = enabled_

  def range = range_

  def whitelist = whitelist_

  def placer = placer_

  def setPlacer(p: EntityPlayer) = if (placer_.isEmpty) {
    whitelist_ = whitelist + p.getName
    placer_ = Some(p.getName)
  }

  private def send(channel: String, args: Any*) = {
    val args2 = args.map(_.asInstanceOf[AnyRef])
    if (getWorld.isRemote) {
      NetworkMessage.sendToServer(this, channel, args2: _*)
    } else {
      NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, channel, args2: _*)
    }
  }

  private def cost():Boolean= {
    if( {energy-= range_ *range_;energy>0}) {
      return true
    }
    energy=0
    false
  }
  private def sync() = {
    assert(!getWorld.isRemote)

    send(MSG_SYNC, range, enabled, whitelist.toArray)
  }

  // Check player in the area and interfere them
  scheduler.every(10).atOnly(Side.SERVER).condition(new Supplier[lang.Boolean] {
    override def get(): lang.Boolean = enabled
  }).run(new Runnable {
    override def run(): Unit = {
      if (cost()) {
        val boundingBox = testBB
        val players = WorldUtils.getEntities(getWorld, boundingBox, EntitySelectors.survivalPlayer)
        players foreach {
          case player: EntityPlayer =>
            CPData.get(player).addInterf(sourceName, new IInterfSource {
              override def interfering(): Boolean =
                boundingBox.contains(new Vec3d(player.posX, player.posY, player.posZ)) &&
                  !TileAbilityInterferer.this.isInvalid &&
                  !player.capabilities.isCreativeMode &&
                  enabled
            })
        }
      }
      else
        enabled_ = false
    }})

  // Sync data to client
  scheduler.every(20).atOnly(Side.SERVER).run(new Runnable {
    override def run(): Unit = sync()
  })

  override def update(){
    super.update()
    scheduler.runTick()
    if(!world.isRemote){
      val stack = this.getStackInSlot(SLOT_BATTERY)
      if (stack != null && EnergyItemHelper.isSupported(stack)) {
        val gain = EnergyItemHelper.pull(stack, Math.min(getMaxEnergy - getEnergy, getBandwidth), false)
        this.injectEnergy(gain)
      }
    }
  }

  @Listener(channel=MSG_SYNC, side=Array(Side.CLIENT))
  private def hSync(range2: Double, enabled2: Boolean, whitelist2 : Array[String]) = {
    range_ = range2
    enabled_ = enabled2
    whitelist_ = SortedSet(whitelist2: _*)
  }

  // Network-cross modifiers
  private def signalFuture(cb: () => Any) = Future.create(new Consumer[Any] {
    override def accept(t: Any): Unit = cb()})

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
    if(tag.hasKey("whitelist_"))
      whitelist_ = SortedSet(NBTS11n.readBase(tag.getTag("whitelist_"), classOf[Array[String]]): _*)
    range_ = tag.getFloat("range_")
  }

  override def writeToNBT(tag: NBTTagCompound): NBTTagCompound = {
    super.writeToNBT(tag)
    tag.setBoolean("enabled_", enabled_)
    tag.setTag("whitelist_", NBTS11n.writeBase(whitelist.toArray))
    tag.setFloat("range_", range_.toFloat)
    tag
  }

  override def getSlotsForFace(side: EnumFacing): Array[Int] = Array[Int](SLOT_BATTERY)

  override def canInsertItem(index: Int, itemStackIn: ItemStack, direction: EnumFacing): Boolean = this.isItemValidForSlot(index, itemStackIn)

  override def canExtractItem(index: Int, stack: ItemStack, direction: EnumFacing): Boolean = false
}