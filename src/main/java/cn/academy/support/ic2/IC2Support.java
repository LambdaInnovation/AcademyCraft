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

import cn.academy.core.AcademyCraft;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.support.BlockConverterBase;
import cn.academy.support.EnergyBlockHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.registry.GameRegistry;
import ic2.api.item.IC2Items;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author KSkun
 */
@Registrant
public class IC2Support {
	
	/**
	 * The convert rate from EU to IF(1IF = <CONV_RATE>EU).
	 */
	public static final double CONV_RATE = 1;

	private static final String MODID = "IC2";
	
	public static double eu2if(double euEnergy) {
		return euEnergy / CONV_RATE;
	}
	
	public static double if2eu(double ifEnergy) {
		return ifEnergy * CONV_RATE;
	}

    @Optional.Method(modid=MODID)
    @RegInitCallback
	public static void init() {
		BlockEUInput euInput = new BlockEUInput();
		BlockEUOutput euOutput = new BlockEUOutput();
		
		try {
			// ACTutorial.addTutorial("energy_bridge_eu").addCondition(Condition.or(Condition.itemsCrafted(euInput,euOutput)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		GameRegistry.registerBlock(euInput, BlockConverterBase.Item.class, "eu_input");
		GameRegistry.registerBlock(euOutput, BlockConverterBase.Item.class, "eu_output");
		
		GameRegistry.registerTileEntity(TileEUInput.class, "eu_input");
		GameRegistry.registerTileEntity(TileEUOutput.class, "eu_output");
		
		EnergyBlockHelper.register(new EUSinkManager());
		EnergyBlockHelper.register(new EUSourceManager());
		
		GameRegistry.addRecipe(new ItemStack(euInput), "abc", " d ",
				'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
				'c', IC2Items.getItem("insulatedCopperCableItem"), 'd', ModuleCrafting.convComp);
		GameRegistry.addRecipe(new ItemStack(euOutput), "abc", " d ",
				'a', IC2Items.getItem("batBox"), 'b', ModuleCrafting.machineFrame,
				'c', IC2Items.getItem("insulatedCopperCableItem"), 'd', ModuleCrafting.convComp);
		
		GameRegistry.addRecipe(new ItemStack(euInput),"X",'X',new ItemStack(euOutput));
		GameRegistry.addRecipe(new ItemStack(euOutput),"X",'X',new ItemStack(euInput));

        AcademyCraft.log.info("IC2 API Support has been loaded.");
	}

}
