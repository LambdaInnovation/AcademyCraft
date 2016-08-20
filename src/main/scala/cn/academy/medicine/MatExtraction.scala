package cn.academy.medicine

import cn.academy.medicine.Properties.Property
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.{Item, ItemStack}
import Properties._
import net.minecraft.block.Block


object MatExtraction {

  case class ItemMeta(item: Item, meta: Int) {
    lazy val id = {
      val itemName = Item.itemRegistry.getNameForObject(item)
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
    ofItem(Items.poisonous_potato) -> List(Var_Fluct),
    ofItem(Items.wheat) -> List(Apply_Continuous_Incr),
    ofItem(Items.reeds) -> List(Targ_MoveSpeed),
    ofBlock(Blocks.cactus) -> List(Str_Mild),
    ofItem(Items.wheat_seeds) -> List(Apply_Continuous_Incr, Str_Mild),
    ofItem(Items.carrot) -> List(Str_Weak),
    ofItem(Items.melon) -> List(Targ_Life),
    ofBlock(Blocks.cocoa) -> List(Targ_Overload, Apply_Continuous_Decr),
    ofItem(Items.egg) -> List(Apply_Instant_Incr),
    ofItem(Items.milk_bucket) -> List(Apply_Continuous_Incr, Var_Stabilize),
    ofItem(Items.feather) -> List(Targ_Jump),
    ofItem(Items.spider_eye) -> List(Targ_Cooldown),
    ofItem(Items.bone) -> List(Str_Mild),
    ofItem(Items.rotten_flesh) -> List(Var_Fluct),
    ofItem(Items.porkchop) -> List(Targ_CP),
    ofItem(Items.cooked_porkchop) -> List(Targ_CP),
    ofItem(Items.chicken) -> List(Targ_CP),
    ofItem(Items.cooked_chicken) -> List(Targ_CP),
    ofItem(Items.beef) -> List(Targ_CP),
    ofItem(Items.cooked_beef) -> List(Targ_CP),
    ofItem(Items.fish) -> List(Apply_Continuous_Incr), // Var_Stablize
    ofItem(Items.cooked_fished) -> List(Apply_Continuous_Incr),
    ofItem(Items.ender_pearl) -> List(Targ_CP, Targ_Overload, Var_Fluct),
    ofItem(Items.blaze_powder) -> List(Targ_Attack, Str_Strong),
    // ofItem(Items.redstone) -> List(Var_Neturalize),
    ofItem(Items.glowstone_dust) -> List(Str_Strong, Var_Desens),
    ofItem(Items.nether_star) -> List(Var_Infinity)
  )

  val allCombinations = allRecipes.flatMap { case (metas, props) =>
      for {
        meta <- metas
        prop <- props
      } yield (meta, prop)
  }

}
