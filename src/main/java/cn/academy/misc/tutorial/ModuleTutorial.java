package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.terminal.ModuleTerminal;
import cn.academy.vanilla.ModuleVanilla;
import cn.lambdalib.annoreg.core.Registrant;
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

    private static ACTutorial defnTut(String name) {
        return TutorialRegistry.addTutorial(name);
    }

	private static void initConditions() {
		defnTut("phase_liquid").setCondition(Condition.harvestLiquid(ModuleCrafting.imagPhase.mat));
        defnTut("constraint_metal").setCondition(Condition.itemPickup(ModuleCrafting.oreConstraintMetal));
        defnTut("crystal").setCondition(Condition.itemPickup(ModuleCrafting.crystalLow));
        defnTut("imag_silicon").setCondition(Condition.itemPickup(ModuleCrafting.oreImagSil));
        defnTut("node").setCondition(Condition.or(
				Condition.itemsCrafted(
						ModuleEnergy.nodeBasic,
						ModuleEnergy.nodeStandard,
						ModuleEnergy.nodeAdvanced
				)
		));
        defnTut("matrix").setCondition(Condition.itemCrafted(ModuleEnergy.matrix));
        defnTut("wifi").setCondition(
				Condition.or(
						Condition.onTutorial("matrix"),
						Condition.onTutorial("node")
				)
		);
        defnTut("phase_generator").setCondition(Condition.itemCrafted(ModuleEnergy.phaseGen));
        defnTut("solar_gen").setCondition(Condition.itemCrafted(ModuleEnergy.solarGen));
        defnTut("wind_gen").setCondition(Condition.itemCrafted(ModuleEnergy.windgenMain));
        defnTut("metal_former").setCondition(Condition.itemCrafted(ModuleCrafting.metalFormer));
        defnTut("imag_fusor").setCondition(Condition.itemCrafted(ModuleCrafting.imagFusor));
        defnTut("terminal").setCondition(Condition.itemCrafted(ModuleTerminal.terminalInstaller));
        defnTut("ability_developer").setCondition(Condition.or(
				Condition.itemsCrafted(
						ModuleAbility.developerNormal,
						ModuleAbility.developerPortable,
						ModuleAbility.developerAdvanced
				)
		));

        defnTut("ability")
                .setCondition(Condition.abilityLevel(null, 1));

        defnTut("ability_electromaster")
				.setCondition(Condition.abilityLevel(ModuleVanilla.electromaster, 1));
        defnTut("ability_meltdowner").setCondition(Condition.abilityLevel(ModuleVanilla.meltdowner, 1));
        defnTut("ability_teleporter").setCondition(Condition.abilityLevel(ModuleVanilla.teleporter, 1));
	}
}
