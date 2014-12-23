package cn.academy.core.register;

import cn.academy.misc.block.ACBlockOre;
import cn.academy.misc.block.dev.BlockAbilityDeveloper;
import cn.academy.misc.block.dev.BlockFieldIncrease;
import cn.academy.misc.block.dev.TileAbilityDeveloper;
import cn.academy.misc.block.dev.TileFieldIncrease;
import cn.academy.misc.block.elec.BlockSolarGenerator;
import cn.academy.misc.block.elec.BlockWindGenerator;
import cn.academy.misc.block.elec.TileSolarGenerator;
import cn.academy.misc.block.elec.TileWindGenerator;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;

public class ACBlocks {
	
	public static Block 
		ability_developer,
		ad_module_magnet,
		windGen,
		solarGen,
		//矿物部分
		copperore,
		tinore,
		aluminumore;
	
	public static void init(Configuration conf) {
		ability_developer = new BlockAbilityDeveloper();
		ad_module_magnet = new BlockFieldIncrease();
		windGen = new BlockWindGenerator();
		solarGen = new BlockSolarGenerator();
		copperore = new ACBlockOre("copperore", 1);
		tinore = new ACBlockOre("tinore", 1);
		aluminumore = new ACBlockOre("aluminumore", 1);
		
		GameRegistry.registerBlock(ability_developer, "ability_developer");
		GameRegistry.registerBlock(ad_module_magnet, "ad_module_fi");
		GameRegistry.registerBlock(windGen, "ad_windgen");
		GameRegistry.registerBlock(solarGen, "ad_solargen");
		
		GameRegistry.registerTileEntity(TileAbilityDeveloper.class, "tile_ability_developer");
		GameRegistry.registerTileEntity(TileFieldIncrease.class, "tile_field_increase");
		GameRegistry.registerTileEntity(TileWindGenerator.class, "tile_windGen");
		GameRegistry.registerTileEntity(TileSolarGenerator.class, "tile_solarGen");
		
		//矿物
		GameRegistry.registerBlock(aluminumore, "aluminumore");
		GameRegistry.registerBlock(copperore, "copper_ore");
		GameRegistry.registerBlock(tinore, "tin_ore");
		
		//矿物词典 
		OreDictionary.registerOre("oreCopper", copperore);
		OreDictionary.registerOre("oreTin", tinore);
		OreDictionary.registerOre("oreAluminum",aluminumore);
	}
}
