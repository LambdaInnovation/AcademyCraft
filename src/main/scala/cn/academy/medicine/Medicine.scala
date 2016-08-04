package cn.academy.medicine

import java.util

import cn.academy.core.item.ACItem
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World

class ItemMedicineBase(name: String) extends ACItem(name) {

  setMaxStackSize(1)

  def create(info: MedicineApplyInfo): ItemStack = {
    val stack = new ItemStack(this)
    MedSynth.writeApplyInfo(stack, info)

    stack
  }

  def getInfo(stack: ItemStack): MedicineApplyInfo = {
    MedSynth.readApplyInfo(stack)
  }

  override def addInformation(stack: ItemStack, player: EntityPlayer, list2: util.List[_], wtf: Boolean): Unit = {
    val list = list2.asInstanceOf[util.List[String]]
    val info = getInfo(stack)

    if (info.target != Properties.Targ_Disposed) {
      list.add(info.target.displayDesc + " " + info.method.displayDesc)
      list.add(info.strengthType.displayDesc)
    } else {
      list.add(EnumChatFormatting.RED + Properties.Targ_Disposed.displayDesc)
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    val info = getInfo(stack)
    info.target.apply(player, info)

    if (!player.capabilities.isCreativeMode) {
      stack.stackSize -= 1
    }

    stack
  }
}

object ItemMedicineBottle extends ItemMedicineBase("medicine_bottle") {

  override def getSubItems(item: Item, cct: CreativeTabs, list2: util.List[_]): Unit = {
    val list = list2.asInstanceOf[util.List[ItemStack]]

    // For debug
    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Normal, 1.0f, Properties.Apply_Instant_Incr)))
    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Normal, 2.0f, Properties.Apply_Instant_Decr)))
  }



}
