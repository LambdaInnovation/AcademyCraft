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
package cn.academy.support.ic2;

import ic2.core.Ic2Items;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameRegistry;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;

/**
 * 
 * @author KSkun
 */
public class IC2Support {
	
	public static void init() {
		BlockEUInput euInput = new BlockEUInput();
		BlockEUOutput euOutput = new BlockEUOutput();
		
		GameRegistry.registerBlock(euInput, "eu_input");
		GameRegistry.registerBlock(euOutput, "eu_output");
		
		GameRegistry.registerTileEntity(TileEUInput.class, "eu_input");
		GameRegistry.registerTileEntity(TileEUOutput.class, "eu_output");
		
		GameRegistry.addRecipe(new ItemStack(euInput), "   ", "abc", " d ",
				'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
				'c', Ic2Items.insulatedCopperCableBlock.getItem(), 
				'd', ModuleCrafting.convComp);
		GameRegistry.addRecipe(new ItemStack(euOutput), "   ", "abc", " d ",
				'a', Ic2Items.batBox.getItem(), 'b', ModuleCrafting.machineFrame,
				'c', Ic2Items.insulatedCopperCableBlock.getItem(),
				'd', ModuleCrafting.convComp);
	}

}
