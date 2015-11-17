package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.core.AcademyCraft;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.terminal.ModuleTerminal;
import cn.academy.vanilla.ModuleVanilla;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;

@Registrant
@RegACRecipeNames
public class ModuleTutorial {
	
	@RegItem
	@RecipeName("tutorial")
	public static ItemTutorial item;

	@RegInitCallback
	public static void init(){
		initConditions();
	}
	
	private static void initConditions() {
		ACTutorial.addTutorial("phase_liquid").setCondition(Condition.harvestLiquid(ModuleCrafting.imagPhase.mat));
		ACTutorial.addTutorial("constraint_metal").setCondition(Condition.itemPickup(ModuleCrafting.oreConstraintMetal));
		ACTutorial.addTutorial("crystal").setCondition(Condition.itemPickup(ModuleCrafting.crystalLow));
		ACTutorial.addTutorial("imag_silicon").setCondition(Condition.itemPickup(ModuleCrafting.oreImagSil));
		ACTutorial.addTutorial("node").setCondition(Condition.or(
				Condition.itemsCrafted(
						ModuleEnergy.nodeBasic,
						ModuleEnergy.nodeStandard,
						ModuleEnergy.nodeAdvanced
				)
		));
		ACTutorial.addTutorial("matrix").setCondition(Condition.itemCrafted(ModuleEnergy.matrix));
		ACTutorial.addTutorial("wifi").setCondition(
				Condition.or(
						Condition.onTutorial("matrix"),
						Condition.onTutorial("node")
				)
		);
		ACTutorial.addTutorial("phase_generator").setCondition(Condition.itemCrafted(ModuleEnergy.phaseGen));
		ACTutorial.addTutorial("solar_gen").setCondition(Condition.itemCrafted(ModuleEnergy.solarGen));
		ACTutorial.addTutorial("wind_gen").setCondition(Condition.itemCrafted(ModuleEnergy.windgenMain));
		ACTutorial.addTutorial("metal_former").setCondition(Condition.itemCrafted(ModuleCrafting.metalFormer));
		ACTutorial.addTutorial("imag_fusor").setCondition(Condition.itemCrafted(ModuleCrafting.imagFusor));
		ACTutorial.addTutorial("terminal").setCondition(Condition.itemCrafted(ModuleTerminal.terminalInstaller));
		ACTutorial.addTutorial("ability_developer").setCondition(Condition.or(
				Condition.itemsCrafted(
						ModuleAbility.developerNormal,
						ModuleAbility.developerPortable,
						ModuleAbility.developerAdvanced
				)
		));

		ACTutorial.addTutorial("ability")
				.setCondition(Condition.abilityLevel(null, 1))
				.setPreview(PreviewHandlers.drawsItem(ModuleCrafting.brainComp),
						PreviewHandlers.drawsBlock(ModuleCrafting.metalFormer, 2), PreviewHandlers.nothing);

		ACTutorial.addTutorial("ability_electromaster")
				.setCondition(Condition.abilityLevel(ModuleVanilla.electromaster, 1));
		ACTutorial.addTutorial("ability_meltdowner").setCondition(Condition.abilityLevel(ModuleVanilla.meltdowner, 1));
		ACTutorial.addTutorial("ability_teleporter").setCondition(Condition.abilityLevel(ModuleVanilla.teleporter, 1));
	}
}
