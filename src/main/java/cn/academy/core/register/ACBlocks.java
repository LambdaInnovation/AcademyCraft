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
package cn.academy.core.register;

import cn.academy.core.block.dev.BlockDeveloper;
import cn.academy.core.block.dev.BlockMagInducer;
import cn.academy.energy.block.BlockMat;
import cn.academy.energy.block.BlockNode;
import cn.academy.energy.block.BlockSolarGenerator;
import cn.academy.misc.block.ACOre;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegBlock;
import cn.liutils.template.block.ItemBlockMulti;

/**
 * AC Blocks Registration Class
 * @author WeathFold, KSkun
 */
@RegistrationClass
public class ACBlocks {
	
	@RegBlock(item = ItemBlockMulti.class)
	public static BlockDeveloper developer;
	
	//@RegBlock
	//TODO: Wait until formal version
	//public static BlockWindGenerator windGen;
	
	@RegBlock
	public static BlockSolarGenerator solarGen;
	
	@RegBlock(item = ItemBlockMulti.class)
	public static BlockMat grid;
	
	@RegBlock
	public static BlockNode node;
	
	@RegBlock(item = ItemBlockMulti.class)
	public static BlockMagInducer magInducer;
	
	//TODO: Not in the plan.
/*	@RegBlock
	@RegBlock.BTName("cuore")
	@RegBlock.OreDict("oreCopper")
	public static ACOre oreCopper = new ACOre(1, 3.05f);*/
	
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
