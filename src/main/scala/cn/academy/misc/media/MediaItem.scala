package cn.academy.misc.media

import java.util

import cn.academy.terminal.TerminalData
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.util._
import net.minecraft.world.World

private object MediaItemInit {
  // TODO: Handle ChestGen
//  import net.minecraftforge.common.ChestGenHooks._
//
//  @RegPreInitCallback
//  def preInit() = {
//    GameRegistry.registerItem(MediaItem, "ac_MediaItem")
//
//    val mediaApperance = List(
//      MINESHAFT_CORRIDOR, PYRAMID_DESERT_CHEST, PYRAMID_JUNGLE_CHEST, STRONGHOLD_LIBRARY, DUNGEON_CHEST)
//
//    for {
//      s <- mediaApperance
//      i <- MediaManager.internalMedias.indices
//    } {
//      val stack = new ItemStack(MediaItem, 1, i)
//      ChestGenHooks.addItem(s, new WeightedRandomChestContent(stack, 1, 1, 4))
//    }
//  }

}

object MediaItem extends Item {

//  var icons: Array[IIcon] = null

  setMaxStackSize(1)
  hasSubtypes = true

  // public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
  override def onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult[ItemStack] = {
    val stack = player.getHeldItem(hand)
    if (!world.isRemote) {
      def sendChat(msg: String, args: AnyRef*) = {
        player.sendMessage(new TextComponentTranslation(msg, args: _*))
      }

      val acquireData = MediaAcquireData(player)
      val tData = TerminalData.get(player)

      val media = getMedia(stack.getItemDamage)

      if (!tData.isInstalled(MediaApp)) {
        sendChat("ac.media.notinstalled")
      } else if (acquireData.isInstalled(media)) {
        sendChat("ac.media.haveone", media.name)
      } else {
        acquireData.install(media)
        if (!player.capabilities.isCreativeMode) {
          stack.shrink(1)
        }
        sendChat("ac.media.acquired", media.name)
      }
    }

    new ActionResult[ItemStack](EnumActionResult.SUCCESS, stack)
  }

//  override def registerIcons(ir : IIconRegister): Unit = {
//    val internal = MediaManager.internalMedias
//    icons = internal.map(media => ir.registerIcon("academy:media_" + media.id)).toArray
//  }

  override def getItemStackDisplayName(stack: ItemStack): String = getMedia(stack.getItemDamage).name

//  @SideOnly(Side.CLIENT)
//  override def getIconFromDamage(meta : Int): IIcon = icons(meta)

  @SideOnly(Side.CLIENT)
  override def addInformation(stack: ItemStack, world: World, tooltip: java.util.List[String], flag: ITooltipFlag): Unit = {
    tooltip.add(getMedia(stack.getItemDamage).desc)
  }

  override def getSubItems(tab: CreativeTabs, items: NonNullList[ItemStack])
  {
    MediaManager.internalMedias.indices.foreach(i => items.add(new ItemStack(this, 1, i)))
  }

  private def getMedia(damage: Int): Media = MediaManager.allMedias(damage)
}