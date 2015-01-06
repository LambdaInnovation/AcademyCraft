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
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.core.ctor.Arg;
import cn.annoreg.core.ctor.Ctor;
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
	@RegBlock.OreDict("oreCopper")
	@Ctor({@Arg(Str = "copperore"), @Arg(Int = 1)})
	public static ACBlockOre copperore;
	
	@RegBlock
	@RegBlock.OreDict("oreTin")
	@Ctor({@Arg(Str = "tinore"), @Arg(Int = 1)})
	public static ACBlockOre tinore;
	
	@RegBlock
	@RegBlock.OreDict("oreAluminum")
	@Ctor({@Arg(Str = "aluminumore"), @Arg(Int = 1)})
	public static ACBlockOre aluminumore;
	
}
