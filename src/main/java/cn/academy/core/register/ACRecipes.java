package cn.academy.core.register;

import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Recipes and smeltings registry
 */
public class ACRecipes {

	public static void regRecipe() {
		
	}
	
	public static void regSmelting() {
		//Ore Smelting
		GameRegistry.addSmelting(ACBlocks.oreAl, new ItemStack(ACItems.ingotAl), 0.7f);
		GameRegistry.addSmelting(ACBlocks.oreCopper, new ItemStack(ACItems.ingotCu), 0.7f);
		GameRegistry.addSmelting(ACBlocks.oreMg, new ItemStack(ACItems.ingotMg), 0.8f);
		GameRegistry.addSmelting(ACBlocks.oreNi, new ItemStack(ACItems.ingotNi), 0.8f);
		GameRegistry.addSmelting(ACBlocks.oreTin, new ItemStack(ACItems.ingotTin), 0.7f);
	}
	
}
