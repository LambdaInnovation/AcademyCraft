package cn.academy.core.register;

import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.block.dev.BlockDeveloper;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.misc.block.ACOre;
import cn.academy.misc.block.energy.BlockGrid;
import cn.academy.misc.block.energy.BlockNode;
import cn.academy.misc.block.energy.BlockSolarGenerator;
import cn.academy.misc.block.energy.BlockWindGenerator;
import cn.academy.misc.block.energy.tile.impl.TileSolarGenerator;
import cn.academy.misc.block.energy.tile.impl.TileWindGenerator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegBlock;
import cpw.mods.fml.common.registry.GameRegistry;

@RegistrationClass
public class ACBlocks {
	
	@RegBlock
	public static BlockDeveloper developer;
	
	@RegBlock
	public static BlockWindGenerator windGen;
	
	@RegBlock
	public static BlockSolarGenerator solarGen;
	
	@RegBlock
	public static BlockGrid grid;
	
	@RegBlock
	public static BlockNode node;
	
	@RegBlock
	@RegBlock.OreDict("oreCopper")
	public static ACOre oreCopper = new ACOre("copperore", 1);
	
	@RegBlock
	@RegBlock.OreDict("oreTin")
	public static ACOre oreTin = new ACOre("tinore", 1);
	
	@RegBlock
	@RegBlock.OreDict("oreAluminum")
	public static ACOre oreAl = new ACOre("aluminumore", 1);
	
	@RegBlock
	@RegBlock.OreDict("oreMg")
	public static ACOre oreMg = new ACOre("mg_ore", 1);
	
	@RegBlock
	@RegBlock.OreDict("oreNi")
	public static ACOre oreNi = new ACOre("ni_ore", 1);
	
	@RegBlock
	@RegBlock.OreDict("oreCrystal")
	public static ACOre oreCrystal = new ACOre("crystal_ore", 1);
	
}
