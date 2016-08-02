package cn.academy.medicine

import cn.academy.medicine.Properties.Property
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.{Item, ItemStack}
import Properties._
import net.minecraft.block.Block


object MatExtraction {

  case class ItemMeta(item: Item, meta: Int) {
    lazy val id = {
      val itemName = item.getUnlocalizedName.drop(5)
      if (item.getHasSubtypes) itemName + "_" + meta else itemName
    }
  }

  type Recipe = (List[ItemMeta], List[Property])

  def ofBlock(block: Block) = ofItem(Item.getItemFromBlock(block))
  def ofBlock(block: Block, damages: Int*) = ofItem(Item.getItemFromBlock(block), damages: _*)

  def ofItem(item: Item): List[ItemMeta] = ofItem(item, 0 to item.getMaxDamage: _*)
  def ofItem(item: Item, damages: Int*): List[ItemMeta] = damages.map(ItemMeta(item, _)).toList

  val allRecipes: List[Recipe] = List(
    ofItem(Items.apple) -> List(Targ_Life, Str_Normal),
    ofItem(Items.potato) -> List(Targ_CP),
    // ofItem(Items.poisonous_potato) -> List(Targ_)
    ofItem(Items.wheat) -> List(Apply_Continuous_Incr),
    ofItem(Items.reeds) -> List(Targ_MoveSpeed),
    ofBlock(Blocks.cactus) -> List(Str_Mild),
    ofItem(Items.wheat_seeds) -> List(Apply_Continuous_Incr, Str_Mild),
    ofItem(Items.carrot) -> List(Str_Weak),
    ofItem(Items.melon) -> List(Targ_Life),
    ofBlock(Blocks.cocoa) -> List(Targ_Overload, Apply_Continuous_Decr),
    ofItem(Items.egg) -> List(Apply_Instant_Incr),
    ofItem(Items.milk_bucket) -> List(Apply_Continuous_Incr),
    ofItem(Items.feather) -> List(Targ_Jump),
    ofItem(Items.spider_eye) -> List(Targ_Cooldown)
  )

  val allCombinations = allRecipes.flatMap { case (metas, props) =>
      for {
        meta <- metas
        prop <- props
      } yield (meta, prop)
  }

}
