/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.util;

import java.util.Arrays;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.register.ACBlocks;
import cn.academy.misc.entity.EntityMagHook;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;

/**
 * Some generic minecraft-world related judging methods that are used globally.
 * @author WeathFolD
 */
@RegistrationClass
@RegSubmoduleInit()
public class JudgeUtils {
	
	/**
	 * Dummy method in order to load the class.
	 */
	public static void init() {}

	public static boolean isOreBlock(Block block) {
		if(block instanceof BlockOre) {
			return true;
		}
		
		if(Item.getItemFromBlock(block) == null)
			return false;
		ItemStack stack = new ItemStack(block);
		int[] val = OreDictionary.getOreIDs(stack);
		for(int i : val) {
			if(OreDictionary.getOreName(i).contains("ore"))
				return true;
		}
		return false;
	}
	
	private static HashSet<Block> metalBlocks = new HashSet();
	static {
		metalBlocks.addAll(Arrays.asList(new Block[] {
			ACBlocks.developer,
			ACBlocks.grid,
			ACBlocks.magInducer,
			ACBlocks.oreNi,
			ACBlocks.solarGen,
			Blocks.rail,
			Blocks.dispenser,
			Blocks.hopper,
			Blocks.iron_bars,
			Blocks.iron_block,
			Blocks.iron_door,
			Blocks.iron_ore,
			Blocks.activator_rail,
		}));
	}
	
	private static HashSet<Class<? extends Entity>> metalEntities = new HashSet();
	static {
		metalEntities.add(EntityMinecart.class);
		metalEntities.add(EntityMagHook.class);
		metalEntities.add(EntityIronGolem.class);
	}
	
	public static boolean isMetalBlock(Block block) {
		return metalBlocks.contains(block);
	}
	
	public static boolean isEntityMetallic(Entity ent) {
		for(Class<? extends Entity> cl : metalEntities) {
			if(cl.isInstance(ent))
				return true;
		}
		return false;
	}

}
