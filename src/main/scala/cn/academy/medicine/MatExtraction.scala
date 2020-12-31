package cn.academy.medicine

import cn.academy.medicine.Properties.Property
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.{Item, ItemStack}
import Properties._
import net.minecraft.block.Block


object MatExtraction {

  case class ItemMeta(item: Item, meta: Int) {
    lazy val id = {
//      val itemName = Item.itemRegistry.getNameForObject(item)
//      if (item.getHasSubtypes) itemName + "_" + meta else itemName
      Item.getIdFromItem(Item)
    }
  }

  type Recipe = (List[ItemMeta], List[Property])

  def ofBlock(block: Block) = ofItem(Item.getItemFromBlock(block))
  def ofBlock(block: Block, damages: Int*) = ofItem(Item.getItemFromBlock(block), damages: _*)

  def ofItem(item: Item): List[ItemMeta] = ofItem(item, 0 to item.getMaxDamage: _*)
  def ofItem(item: Item, damages: Int*): List[ItemMeta] = damages.map(ItemMeta(item, _)).toList

  val allRecipes: List[Recipe] = List(
    ofItem(Items.APPLE) -> List(Targ_Life, Str_Normal),
    ofItem(Items.POTATO) -> List(Targ_CP),
    ofItem(Items.POISONOUS_POTATO) -> List(Var_Fluct),
    ofItem(Items.WHEAT) -> List(Apply_Continuous_Incr),
    ofItem(Items.REEDS) -> List(Targ_MoveSpeed),
    ofBlock(Blocks.CACTUS) -> List(Str_Mild),
    ofItem(Items.WHEAT_SEEDS) -> List(Apply_Continuous_Incr, Str_Mild),
    ofItem(Items.CARROT) -> List(Str_Weak),
    ofItem(Items.MELON) -> List(Targ_Life),
    ofBlock(Blocks.COCOA) -> List(Targ_Overload, Apply_Continuous_Decr),
    ofItem(Items.EGG) -> List(Apply_Instant_Incr),
    ofItem(Items.MILK_BUCKET) -> List(Apply_Continuous_Incr, Var_Stabilize),
    ofItem(Items.FEATHER) -> List(Targ_Jump),
    ofItem(Items.SPIDER_EYE) -> List(Targ_Cooldown),
    ofItem(Items.BONE) -> List(Str_Mild),
    ofItem(Items.ROTTEN_FLESH) -> List(Var_Fluct),
    ofItem(Items.PORKCHOP) -> List(Targ_CP),
    ofItem(Items.COOKED_PORKCHOP) -> List(Targ_CP),
    ofItem(Items.CHICKEN) -> List(Targ_CP),
    ofItem(Items.COOKED_CHICKEN) -> List(Targ_CP),
    ofItem(Items.BEEF) -> List(Targ_CP),
    ofItem(Items.COOKED_BEEF) -> List(Targ_CP),
    ofItem(Items.FISH) -> List(Apply_Continuous_Incr), // Var_Stablize
    ofItem(Items.COOKED_FISH) -> List(Apply_Continuous_Incr),
    ofItem(Items.ENDER_PEARL) -> List(Targ_CP, Targ_Overload, Var_Fluct),
    ofItem(Items.BLAZE_POWDER) -> List(Targ_Attack, Str_Strong),
    // ofItem(Items.redstone) -> List(Var_Neturalize),
    ofItem(Items.GLOWSTONE_DUST) -> List(Str_Strong, Var_Desens),
    ofItem(Items.NETHER_STAR) -> List(Var_Infinity)
  )

  val allCombinations = allRecipes.flatMap { case (metas, props) =>
      for {
        meta <- metas
        prop <- props
      } yield (meta, prop)
  }

}
