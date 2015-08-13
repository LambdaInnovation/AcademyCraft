package cn.academy.support;

import cofh.thermalexpansion.block.cell.BlockCell;
import cofh.thermalexpansion.item.TEItems;
import net.minecraft.item.ItemStack;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.support.te.BlockRFInput;
import cn.academy.support.te.BlockRFOutput;
import cn.academy.support.te.TileRFInput;
import cn.academy.support.te.TileRFOutput;
import cpw.mods.fml.common.registry.GameRegistry;

public class TESupport {
	
	public static void init() {
		BlockRFInput rfInput = new BlockRFInput();
		BlockRFOutput rfOutput = new BlockRFOutput();
		
		GameRegistry.registerBlock(rfInput, "rf_input");
		GameRegistry.registerBlock(rfOutput, "rf_output");
		
		GameRegistry.registerTileEntity(TileRFInput.class, "rf_input");
		GameRegistry.registerTileEntity(TileRFOutput.class, "rf_output");
		
		GameRegistry.addRecipe(new ItemStack(rfInput), "   ", "abc", "d",
				'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
				'c', TEItems.powerCoilGold.getItem(), 'd', ModuleCrafting.convComp);
		GameRegistry.addRecipe(new ItemStack(rfInput), "   ", "abc", "d",
				'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
				'c', BlockCell.cellBasic.getItem(), 'd', ModuleCrafting.convComp);
	}
	
}
