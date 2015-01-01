package cn.academy.core.register;

import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.block.BlockDeveloper;
import cn.academy.core.block.TileDeveloper;
import cn.academy.misc.block.ACBlockOre;
import cn.academy.misc.block.elec.BlockSolarGenerator;
import cn.academy.misc.block.elec.BlockWindGenerator;
import cn.academy.misc.block.elec.TileSolarGenerator;
import cn.academy.misc.block.elec.TileWindGenerator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.core.ctor.Arg;
import cn.annoreg.core.ctor.Ctor;
import cn.annoreg.mc.RegBlock;
import cpw.mods.fml.common.registry.GameRegistry;

@RegistrationClass
public class ACBlocks {
	
	//public static Block 
		//developer,
		//windGen,
		//solarGen,
		//矿物部分
		//copperore,
		//tinore,
		//aluminumore;
	
	@RegBlock(name = "ac_developer")
	public static BlockDeveloper developer;
	
	@RegBlock(name = "ac_windgen")
	public static BlockWindGenerator windGen;
	
	@RegBlock(name = "ac_solargen")
	public static BlockSolarGenerator solarGen;
	
	@RegBlock(name = "copper_ore")
	@RegBlock.OreDict("oreCopper")
	@Ctor({@Arg(Str = "copperore"), @Arg(Int = 1)})
	public static ACBlockOre copperore;
	
	@RegBlock(name = "tin_ore")
	@RegBlock.OreDict("oreTin")
	@Ctor({@Arg(Str = "tinore"), @Arg(Int = 1)})
	public static ACBlockOre tinore;
	
	@RegBlock(name = "aluminumore")
	@RegBlock.OreDict("oreAluminum")
	@Ctor({@Arg(Str = "aluminumore"), @Arg(Int = 1)})
	public static ACBlockOre aluminumore;
	
	public static void init(Configuration conf) {
		//developer = new BlockDeveloper();
		//windGen = new BlockWindGenerator();
		//solarGen = new BlockSolarGenerator();
		//copperore = new ACBlockOre("copperore", 1);
		//tinore = new ACBlockOre("tinore", 1);
		//aluminumore = new ACBlockOre("aluminumore", 1);
		
		//GameRegistry.registerBlock(developer, "ac_developer");
		//GameRegistry.registerBlock(windGen, "ac_windgen");
		//GameRegistry.registerBlock(solarGen, "ac_solargen");

		GameRegistry.registerTileEntity(TileDeveloper.class, "tile_acdev");
		GameRegistry.registerTileEntity(TileWindGenerator.class, "tile_windGen");
		GameRegistry.registerTileEntity(TileSolarGenerator.class, "tile_solarGen");
		
		//矿物
		//GameRegistry.registerBlock(aluminumore, "aluminumore");
		//GameRegistry.registerBlock(copperore, "copper_ore");
		//GameRegistry.registerBlock(tinore, "tin_ore");
		
		//矿物词典 
		//OreDictionary.registerOre("oreCopper", copperore);
		//OreDictionary.registerOre("oreTin", tinore);
		//OreDictionary.registerOre("oreAluminum",aluminumore);
	}
}
