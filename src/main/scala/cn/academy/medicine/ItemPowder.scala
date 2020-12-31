package cn.academy.medicine

import cn.academy.medicine.MatExtraction.ItemMeta
import cn.academy.medicine.Properties.Property

import java.util
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.fml.common.registry.GameRegistry


//object ItemPowder {
//
//  def getProperty(stack: ItemStack): Property = stack.getItem match {
//    case item: ItemPowder => item.prop
//    case _ => throw new IllegalArgumentException("Given itemStack is not a powder")
//  }
//
//  def _init() = {
//    MatExtraction.allCombinations.foreach { case (src, prop) =>
//      val item = new ItemPowder(src, prop)
//      GameRegistry.registerItem(item, item.internalID)
//    }
//  }
//
//}

class ItemPowder(val source: ItemMeta, val prop: Property) extends Item {

//  val dummyStack = new ItemStack(source.item, 1, source.meta)

//  setTextureName("academy:powder/" + internalID)

//  def internalID = source.id.dropWhile(_ != ':').drop(1) + "_" + prop.internalID

//  override def getItemStackDisplayName(stack: ItemStack): String = {
//    source.item.getItemStackDisplayName(dummyStack) + " " + super.getItemStackDisplayName(stack)
//  }

//  override def addInformation(stack: ItemStack, player: EntityPlayer, list2: util.List[_], wtf: Boolean): Unit = {
//    val list = list2.asInstanceOf[util.List[String]]
//    list.add(prop.stackDisplayHint)
//  }
}