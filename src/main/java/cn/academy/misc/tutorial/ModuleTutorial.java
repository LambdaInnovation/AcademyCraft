package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.Category;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.CatElectromaster;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

@Registrant
@RegInit
public class ModuleTutorial {
	public static void init(){
		try {
			ACTutorial.addTutorial("phase_liquid").addCondition(Condition.harvestLiquid(ModuleCrafting.imagPhase.mat));
			ACTutorial.addTutorial("constraint_metal").addCondition(Condition.itemPickup(Item.getItemFromBlock(ModuleCrafting.oreConstraintMetal)));
			ACTutorial.addTutorial("crystal").addCondition(Condition.itemPickup(ModuleCrafting.crystalLow));
			ACTutorial.addTutorial("imag_silicon").addCondition(Condition.itemPickup(Item.getItemFromBlock(ModuleCrafting.oreImagSil)));
			ACTutorial.addTutorial("node").addCondition(Condition.or(
					Condition.itemsCrafted(
							Item.getItemFromBlock(ModuleEnergy.nodeBasic),
							Item.getItemFromBlock(ModuleEnergy.nodeStandard),
							Item.getItemFromBlock(ModuleEnergy.nodeAdvanced)
							)
					));
			ACTutorial.addTutorial("matrix").addCondition(Condition.itemCrafted(Item.getItemFromBlock(ModuleEnergy.matrix)));
			ACTutorial.addTutorial("WiFi");//未完成
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
