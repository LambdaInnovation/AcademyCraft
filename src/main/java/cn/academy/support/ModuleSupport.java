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
package cn.academy.support;

import cpw.mods.fml.common.registry.GameRegistry;
import cn.academy.support.ic2.BlockEUInput;
import cn.academy.support.ic2.BlockEUOutput;
import cn.academy.support.ic2.TileEUInput;
import cn.academy.support.ic2.TileEUOutput;
import cn.liutils.crafting.RecipeRegistry;

/**
 * 
 * @author KSkun
 */
public class ModuleSupport {
	
	public static void initIC2(RecipeRegistry recipes) {
		BlockEUInput euInput = new BlockEUInput();
		BlockEUOutput euOutput = new BlockEUOutput();
		
		
		GameRegistry.registerBlock(euInput, "eu_input");
		GameRegistry.registerBlock(euOutput, "eu_output");
		
		GameRegistry.registerTileEntity(TileEUInput.class, "eu_input");
		GameRegistry.registerTileEntity(TileEUOutput.class, "eu_output");
		
		recipes.map("eu_input", euInput);
		recipes.map("eu_output", euOutput);
	}

}
