package cn.academy.core.register;

import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.block.dev.BlockDeveloper;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.misc.block.ACBlockOre;
import cn.academy.misc.block.elec.BlockSolarGenerator;
import cn.academy.misc.block.elec.BlockWindGenerator;
import cn.academy.misc.block.elec.TileSolarGenerator;
import cn.academy.misc.block.elec.TileWindGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

public class ACBlocks {
	
	public static Block 
		developer,
		windGen,
		solarGen,
		//矿物部分
		copperore,
		tinore,
		aluminumore;
	
	public static void init(Configuration conf) {
		developer = new BlockDeveloper();
		windGen = new BlockWindGenerator();
		solarGen = new BlockSolarGenerator();
		copperore = new ACBlockOre("copperore", 1);
		tinore = new ACBlockOre("tinore", 1);
		aluminumore = new ACBlockOre("aluminumore", 1);
		
		GameRegistry.registerBlock(developer, "ac_developer");
		GameRegistry.registerBlock(windGen, "ac_windgen");
		GameRegistry.registerBlock(solarGen, "ac_solargen");

		GameRegistry.registerTileEntity(TileDeveloper.class, "tile_acdev");
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
