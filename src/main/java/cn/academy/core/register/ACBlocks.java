package cn.academy.core.register;

import cn.academy.core.block.dev.BlockDeveloper;
import cn.academy.core.block.dev.BlockMagInducer;
import cn.academy.energy.block.BlockMat;
import cn.academy.energy.block.BlockNode;
import cn.academy.energy.block.BlockSolarGenerator;
import cn.academy.misc.block.ACOre;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegBlock;

/**
 * AC Blocks Registration Class
 * @author LI Members
 */
@RegistrationClass
public class ACBlocks {
	
	@RegBlock
	public static BlockDeveloper developer;
	
	//@RegBlock
	//TODO: Wait until formal version
	//public static BlockWindGenerator windGen;
	
	@RegBlock
	public static BlockSolarGenerator solarGen;
	
	@RegBlock
	public static BlockMat grid;
	
	@RegBlock
	public static BlockNode node;
	
	@RegBlock
	public static BlockMagInducer magInducer;
	
	@RegBlock
	@RegBlock.BTName("cuore")
	@RegBlock.OreDict("oreCopper")
	public static ACOre oreCopper = new ACOre(1, 3.05f);
	
	//TODO: This block isn't in used in AC beta.
/*	@RegBlock
	@RegBlock.BTName("tinore")
	@RegBlock.OreDict("oreTin")
	public static ACOre oreTin = new ACOre(1, 2.95f);*/
	
	@RegBlock
	@RegBlock.BTName("alore")
	@RegBlock.OreDict("oreAluminium")
	public static ACOre oreAl = new ACOre(1, 2.9f);
	
	@RegBlock
	@RegBlock.BTName("mgore")
	@RegBlock.OreDict("oreMagnesium")
	public static ACOre oreMg = new ACOre(1, 2.95f);
	
	@RegBlock
	@RegBlock.BTName("niore")
	@RegBlock.OreDict("oreNickel")
	public static ACOre oreNi = new ACOre(1, 3.2f);
	
	@RegBlock
	@RegBlock.BTName("crystalore")
	@RegBlock.OreDict("oreCrystal")
	public static ACOre oreCrystal = new ACOre(2, 3.5f);
	
	@RegBlock
	@RegBlock.BTName("shadowore")
	@RegBlock.OreDict("oreShadow")
	public static ACOre oreShadow = new ACOre(2, 3.5f).setLightLevel(0.9375f);
	
}
