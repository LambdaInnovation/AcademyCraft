package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.terminal.ModuleTerminal;
import cn.academy.vanilla.ModuleVanilla;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegPostInitCallback;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;

import static cn.academy.misc.tutorial.Condition.*;
import static cn.academy.misc.tutorial.PreviewHandlers.*;

@Registrant
@RegACRecipeNames
public class ModuleTutorial {
	
	@RegItem
	@RecipeName("tutorial")
	public static ItemTutorial item;

    private static ACTutorial defnTut(String name) {
        return TutorialRegistry.addTutorial(name);
    }

    @RegPostInitCallback
	public static void initConditions() {

		defnTut("phase_liquid").setCondition(harvestLiquid(ModuleCrafting.imagPhase.mat));

        defnTut("constraint_metal").setCondition(itemPickup(ModuleCrafting.oreConstraintMetal));

        defnTut("crystal").setCondition(itemPickup(ModuleCrafting.crystalLow))
                .addPreview(PreviewHandlers.plainDisplay(ModuleCrafting.crystalLow));

        defnTut("imag_silicon") .setCondition(itemPickup(ModuleCrafting.oreImagSil));

        defnTut("node") .setCondition(or(
				itemsCrafted(
						ModuleEnergy.nodeBasic,
						ModuleEnergy.nodeStandard,
						ModuleEnergy.nodeAdvanced
				)
		)).addPreview(plainDisplay(ModuleEnergy.nodeBasic))
        .addPreview(plainDisplay(ModuleEnergy.nodeStandard))
        .addPreview(plainDisplay(ModuleEnergy.nodeAdvanced));

        defnTut("matrix")
                .setCondition(itemCrafted(ModuleEnergy.matrix))
                .addPreview(plainDisplay(ModuleEnergy.matrix));

        defnTut("wifi")
                .setCondition(or(onTutorial("matrix"), onTutorial("node")))
                .addPreview(plainDisplay(ModuleEnergy.matrix));

        defnTut("phase_generator")
                .setCondition(itemCrafted(ModuleEnergy.phaseGen))
                .addPreview(plainDisplay(ModuleEnergy.phaseGen));

        defnTut("solar_gen")
                .setCondition(Condition.itemCrafted(ModuleEnergy.solarGen))
                .addPreview(plainDisplay(ModuleEnergy.solarGen));

        defnTut("wind_gen").setCondition(Condition.itemCrafted(ModuleEnergy.windgenMain));

        defnTut("metal_former").setCondition(Condition.itemCrafted(ModuleCrafting.metalFormer));

        defnTut("imag_fusor").setCondition(Condition.itemCrafted(ModuleCrafting.imagFusor));

        defnTut("terminal").setCondition(Condition.itemCrafted(ModuleTerminal.terminalInstaller));

        defnTut("ability_developer").setCondition(or(
				itemsCrafted(
						ModuleAbility.developerNormal,
						ModuleAbility.developerPortable,
						ModuleAbility.developerAdvanced
				)
		));

        defnTut("ability")
                .setCondition(abilityLevel(null, 1));

        defnTut("ability_electromaster")
				.setCondition(abilityLevel(ModuleVanilla.electromaster, 1));

        defnTut("ability_meltdowner").setCondition(abilityLevel(ModuleVanilla.meltdowner, 1));

        defnTut("ability_teleporter").setCondition(abilityLevel(ModuleVanilla.teleporter, 1));
	}
}
