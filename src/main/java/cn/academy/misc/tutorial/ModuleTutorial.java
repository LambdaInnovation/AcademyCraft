package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.Category;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.knowledge.ModuleKnowledge;
import cn.academy.misc.ModuleMisc;
import cn.academy.terminal.ModuleTerminal;
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
			ACTutorial.addTutorial("constraint_metal").addCondition(Condition.itemPickup(ModuleCrafting.oreConstraintMetal));
			ACTutorial.addTutorial("crystal").addCondition(Condition.itemPickup(ModuleCrafting.crystalLow));
			ACTutorial.addTutorial("imag_silicon").addCondition(Condition.itemPickup(ModuleCrafting.oreImagSil));
			ACTutorial.addTutorial("node").addCondition(Condition.or(
					Condition.itemsCrafted(
							ModuleEnergy.nodeBasic,
							ModuleEnergy.nodeStandard,
							ModuleEnergy.nodeAdvanced
							)
					));
			ACTutorial.addTutorial("matrix").addCondition(Condition.itemCrafted(ModuleEnergy.matrix));
			ACTutorial.addTutorial("WiFi").addCondition(
					Condition.or(
							Condition.onTutorial("matrix"),
							Condition.onTutorial("node")
							)
					);
			ACTutorial.addTutorial("phase_generator").addCondition(Condition.itemCrafted(ModuleEnergy.phaseGen));
			ACTutorial.addTutorial("solar_gen").addCondition(Condition.itemCrafted(ModuleEnergy.solarGen));
			ACTutorial.addTutorial("wind_gen").addCondition(Condition.itemCrafted(ModuleEnergy.windgenMain));
			ACTutorial.addTutorial("metal_former").addCondition(Condition.itemCrafted(ModuleCrafting.metalFormer));
			ACTutorial.addTutorial("imag_fusor").addCondition(Condition.itemCrafted(ModuleCrafting.imagFusor));
			ACTutorial.addTutorial("terminal").addCondition(Condition.itemCrafted(ModuleTerminal.terminalInstaller));
			ACTutorial.addTutorial("ability_developer").addCondition(Condition.or(
					Condition.itemsCrafted(
							ModuleAbility.developerNormal,
							ModuleAbility.developerPortable,
							ModuleAbility.developerAdvanced)
					));
			ACTutorial.addTutorial("ability").addCondition(Condition.abilityLevel(null, 1));
			ACTutorial.addTutorial("ability_electromaster").addCondition(Condition.abilityLevel(ModuleVanilla.electromaster, 1));
			ACTutorial.addTutorial("ability_meltdowner").addCondition(Condition.abilityLevel(ModuleVanilla.meltdowner, 1));
			ACTutorial.addTutorial("ability_teleporter").addCondition(Condition.abilityLevel(ModuleVanilla.teleporter, 1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
