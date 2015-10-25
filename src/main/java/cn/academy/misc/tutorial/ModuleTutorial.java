package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.core.AcademyCraft;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.terminal.ModuleTerminal;
import cn.academy.vanilla.ModuleVanilla;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;
import cn.liutils.crafting.CustomMappingHelper.RecipeName;

@Registrant
@RegInit
@RegACRecipeNames
public class ModuleTutorial {
	
	@RegItem
	@RecipeName("tutorial")
	public static ItemTutorial item;
	
	public static void init(){
		// initConditions();
	}
	
	private void initConditions() {
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
			ACTutorial.addTutorial("wifi").addCondition(
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
			AcademyCraft.log.error("Registering AC tutorial conditions", e);
		}
	}
}
