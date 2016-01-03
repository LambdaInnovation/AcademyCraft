package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.terminal.ModuleTerminal;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegPostInitCallback;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.vis.model.CompTransform;

import javax.vecmath.Vector2f;

import java.util.Optional;

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
        defnTut("welcome");

        defnTut("phase_liquid")
            // .setCondition(harvestLiquid(ModuleCrafting.imagPhase.mat))
            .addPreview(displayIcon("items/matter_unit/phase_liquid_mat",
                                new Vector2f(0, 0), 1, Color.white()))
            .addPreview(recipes(ModuleCrafting.matterUnit));

        defnTut("constraint_metal")
                .setCondition(itemPickup(ModuleCrafting.oreConstraintMetal))
                .addPreview(drawsBlock(ModuleCrafting.oreConstraintMetal))
                .addPreview(drawsItem(ModuleCrafting.ingotConst))
                .addPreview(drawsItem(ModuleCrafting.constPlate))
                .addPreview(recipes(ModuleCrafting.ingotConst))
                .addPreview(recipes(ModuleCrafting.constPlate));

        defnTut("crystal")
                .setCondition(itemPickup(ModuleCrafting.crystalLow))
                .addPreview(drawsItem(ModuleCrafting.crystalLow))
                .addPreview(drawsItem(ModuleCrafting.crystalNormal))
                .addPreview(drawsItem(ModuleCrafting.crystalPure))
                .addPreview(recipes(ModuleCrafting.crystalLow))
                .addPreview(recipes(ModuleCrafting.crystalNormal))
                .addPreview(recipes(ModuleCrafting.crystalPure));

        defnTut("imag_silicon")
                .setCondition(itemPickup(ModuleCrafting.oreImagSil))
                .addPreview(drawsBlock(ModuleCrafting.oreImagSil))
                .addPreview(drawsItem(ModuleCrafting.ingotImagSil))
                .addPreview(recipes(ModuleCrafting.oreImagSil));

        defnTut("node").setCondition(or(
                itemsCrafted(
                        ModuleEnergy.nodeBasic,
                        ModuleEnergy.nodeStandard,
                        ModuleEnergy.nodeAdvanced
                )
        )).addPreview(drawsBlock(ModuleEnergy.nodeBasic))
        .addPreview(recipes(ModuleEnergy.nodeBasic))
        .addPreview(recipes(ModuleEnergy.nodeStandard))
        .addPreview(recipes(ModuleEnergy.nodeAdvanced));

        defnTut("matrix")
                .setCondition(itemCrafted(ModuleEnergy.matrix))
                .addPreview(displayModel("matrix", "matrix",
                        new CompTransform()
                                .setTransform(0, -0.45, 0)
                                .setScale(0.3)))
                .addPreview(recipes(ModuleEnergy.matrix));

        defnTut("wireless_network")
                .setCondition(or(onTutorial("matrix"), onTutorial("node")));

        defnTut("phase_generator")
                .setCondition(itemCrafted(ModuleEnergy.phaseGen))
                .addPreview(displayModel("ip_gen", "ip_gen0",
                        new CompTransform().setScale(0.6).setTransform(0, -0.35, 0)))
                .addPreview(recipes(ModuleEnergy.phaseGen));

        defnTut("solar_gen")
                .setCondition(Condition.itemCrafted(ModuleEnergy.solarGen))
                .addPreview(displayModel("solar",
                        new CompTransform().setScale(0.01).setTransform(0, -0.28, 0)))
                .addPreview(recipes(ModuleEnergy.solarGen));

        defnTut("wind_gen")
                .setCondition(Condition.itemCrafted(ModuleEnergy.windgenMain))
                .addPreview(displayModel("windgen_base",
                        new CompTransform().setScale(0.5).setTransform(0, -0.5, 0)))
                .addPreview(displayModel("windgen_fan",
                        new CompTransform().setScale(0.1)));

        defnTut("metal_former")
                .setCondition(Condition.itemCrafted(ModuleCrafting.metalFormer))
                .addPreview(drawsBlock(ModuleCrafting.metalFormer))
                .addPreview(recipes(ModuleCrafting.metalFormer));

        defnTut("imag_fusor")
                .setCondition(Condition.itemCrafted(ModuleCrafting.imagFusor))
                .addPreview(drawsBlock(ModuleCrafting.imagFusor))
                .addPreview(recipes(ModuleCrafting.imagFusor));

        defnTut("data_terminal")
                .setCondition(Condition.itemCrafted(ModuleTerminal.terminalInstaller))
                .addPreview(drawsItem(ModuleTerminal.terminalInstaller))
                .addPreview(recipes(ModuleTerminal.terminalInstaller));

        defnTut("ability_developer").setCondition(or(
                itemsCrafted(
                        ModuleAbility.developerNormal,
                        ModuleAbility.developerPortable,
                        ModuleAbility.developerAdvanced
                )
        )).addPreview(drawsItem(ModuleAbility.developerPortable))
            .addPreview(displayModel("developer_advanced",
                    new CompTransform().setScale(0.16).setTransform(0, -0.4, -0.3)))
            .addPreview(recipes(ModuleAbility.developerPortable))
            .addPreview(recipes(ModuleAbility.developerNormal))
            .addPreview(recipes(ModuleAbility.developerAdvanced));

        defnTut("ability")
                .setCondition(abilityLevel(Optional.empty(), 1));
    }
}
