/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
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

import java.util.Optional;

import static cn.academy.misc.tutorial.Conditions.*;
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
            .addPreview(displayIcon("items/matter_unit/phase_liquid_mat",
                                0, 0, 
                                1, Color.white()))
            .addPreview(recipes(ModuleCrafting.matterUnit));

        defnTut("constraint_metal")
                .setCondition(itemObtained(ModuleCrafting.oreConstraintMetal))
                .addPreview(drawsBlock(ModuleCrafting.oreConstraintMetal))
                .addPreview(drawsItem(ModuleCrafting.ingotConst))
                .addPreview(drawsItem(ModuleCrafting.constPlate))
                .addPreview(recipes(ModuleCrafting.ingotConst))
                .addPreview(recipes(ModuleCrafting.constPlate));

        defnTut("crystal")
                .setCondition(itemObtained(ModuleCrafting.crystalLow))
                .addPreview(drawsItem(ModuleCrafting.crystalLow))
                .addPreview(drawsItem(ModuleCrafting.crystalNormal))
                .addPreview(drawsItem(ModuleCrafting.crystalPure))
                .addPreview(recipes(ModuleCrafting.crystalLow))
                .addPreview(recipes(ModuleCrafting.crystalNormal))
                .addPreview(recipes(ModuleCrafting.crystalPure));
    }
}
