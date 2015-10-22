package cn.academy.support.te;

import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.misc.tutorial.ACTutorial;
import cn.academy.misc.tutorial.Condition;
import cn.academy.support.EnergyBlockHelper;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class TESupport {
	
	/** The convert rate (RF * RATE = IF) */
	public static final float CONV_RATE = 1f;
	
	private static Block rfInput, rfOutput;
	
	public static void init() {
		BlockRFInput rfInput = new BlockRFInput();
		BlockRFOutput rfOutput = new BlockRFOutput();
		
		try {
			ACTutorial.addTutorial("energy_bridge_rf").addCondition(Condition.or(Condition.itemsCrafted(rfInput,rfOutput)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GameRegistry.registerBlock(rfInput, "rf_input");
		GameRegistry.registerBlock(rfOutput, "rf_output");
		
		GameRegistry.registerTileEntity(TileRFInput.class, "rf_input");
		GameRegistry.registerTileEntity(TileRFOutput.class, "rf_output");
		
		EnergyBlockHelper.register(new RFProviderManager());
		EnergyBlockHelper.register(new RFReceiverManager());
		
		ItemStack coilGoldStack = new ItemStack(GameRegistry.findItem("ThermalExpansion", "material"), 1, 1),
				coilSilverStack = new ItemStack(GameRegistry.findItem("ThermalExpansion", "material"), 1, 2);
		
		GameRegistry.addRecipe(new ItemStack(rfInput), "abc", " d ",
				'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
				'c', coilGoldStack, 'd', ModuleCrafting.convComp);
		
		GameRegistry.addRecipe(new ItemStack(rfOutput), "abc", " d ",
				'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
				'c', coilSilverStack, 'd', ModuleCrafting.convComp);
	}
	
}
