package cn.academy.misc.media

import java.util

import cn.academy.core.item.ACItem
import cn.academy.terminal.TerminalData
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.{ChatComponentTranslation, IIcon, WeightedRandomChestContent}
import net.minecraft.world.World
import net.minecraftforge.common.ChestGenHooks

private object MediaItemInit {
  import net.minecraftforge.common.ChestGenHooks._

  @RegPreInitCallback
  def preInit() = {
    GameRegistry.registerItem(MediaItem, "ac_MediaItem")

    val mediaApperance = List(
      MINESHAFT_CORRIDOR, PYRAMID_DESERT_CHEST, PYRAMID_JUNGLE_CHEST, STRONGHOLD_LIBRARY, DUNGEON_CHEST)

    for {
      s <- mediaApperance
      i <- MediaManager.internalMedias.indices
    } {
      val stack = new ItemStack(MediaItem, 1, i)
      ChestGenHooks.addItem(s, new WeightedRandomChestContent(stack, 1, 1, 4))
    }
  }

}

object MediaItem extends ACItem("media") {

  var icons: Array[IIcon] = null

  setMaxStackSize(1)
  hasSubtypes = true

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!world.isRemote) {
      def sendChat(msg: String, args: AnyRef*) = {
        player.addChatComponentMessage(new ChatComponentTranslation(msg, args: _*))
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
          stack.stackSize -= 1
        }
        sendChat("ac.media.acquired", media.name)
      }
    }

    stack
  }

  override def registerIcons(ir : IIconRegister): Unit = {
    val internal = MediaManager.internalMedias
    icons = internal.map(media => ir.registerIcon("academy:media_" + media.id)).toArray
  }

  override def getItemStackDisplayName(stack: ItemStack): String = getMedia(stack.getItemDamage).name

  @SideOnly(Side.CLIENT)
  override def getIconFromDamage(meta : Int): IIcon = icons(meta)

  @SideOnly(Side.CLIENT)
  override def addInformation(stack: ItemStack, player: EntityPlayer, list_ : util.List[_], wtf: Boolean): Unit = {
    val list = list_.asInstanceOf[util.List[String]]
    list.add(getMedia(stack.getItemDamage).desc)
  }

  @SideOnly(Side.CLIENT)
  override def getSubItems(item: Item, tab: CreativeTabs, list_ : util.List[_]): Unit = {
    val list = list_.asInstanceOf[util.List[ItemStack]]
    MediaManager.internalMedias.indices.foreach(i => list.add(new ItemStack(this, 1, i)))
  }

  private def getMedia(damage: Int): Media = MediaManager.allMedias(damage)
}