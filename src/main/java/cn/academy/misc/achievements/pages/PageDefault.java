/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.achievements.pages;

import cn.academy.ability.ModuleAbility;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchCrSingle;
import cn.academy.misc.achievements.aches.AchEvItemPickup;
import cn.academy.misc.achievements.aches.AchEvMatterUnitHarvest;
import cn.academy.terminal.ModuleTerminal;
import net.minecraft.item.ItemStack;

/**
 * @author EAirPeter
 */
public final class PageDefault extends ACAchievementPage {

    private final ACAchievement aPhaseLiquid;
    private final ACAchievement aMatrix1;
    private final ACAchievement aMatrix2;
    private final ACAchievement aNode;
    private final ACAchievement aDeveloper1;
    private final ACAchievement aDeveloper2;
    private final ACAchievement aDeveloper3;
    private final ACAchievement aPhaseGen;
    private final ACAchievement aSolarGen;
    private final ACAchievement aWindGen;
    private final ACAchievement aCrystal;
    private final ACAchievement aTerminal;
    
    public PageDefault() {
        super("default");
        add(new ACAchievement[] {
            aPhaseLiquid = new AchEvMatterUnitHarvest("phase_liquid", 0, 0, ModuleCrafting.imagPhase, null, ModuleCrafting.imagPhase),
            aMatrix1 = new AchCrSingle("matrix1", -2, 0, ModuleEnergy.matrix, aPhaseLiquid).adItemCrafted(ModuleEnergy.matrix),
            aMatrix2 = new AchCrSingle("matrix2", -2, -2, ModuleEnergy.matrixCore, aMatrix1).adItemCrafted(ModuleEnergy.matrixCore),
            aNode = new AchCrSingle("node", 0, -2, ModuleEnergy.nodeBasic, aPhaseLiquid).adItemCrafted(ModuleEnergy.nodeBasic),
            aDeveloper1 = new AchCrSingle("developer1", 2, -2, ModuleAbility.developerPortable, aNode).adItemCrafted(ModuleAbility.developerPortable),
            aDeveloper2 = new AchCrSingle("developer2", 4, -2, ModuleAbility.developerNormal, aDeveloper1).adItemCrafted(ModuleAbility.developerNormal),
            aDeveloper3 = new AchCrSingle("developer3", 4, 0, ModuleAbility.developerAdvanced, aDeveloper2).adItemCrafted(ModuleAbility.developerAdvanced),
            aPhaseGen = new AchCrSingle("phasegen", 0, 2, ModuleEnergy.phaseGen, aPhaseLiquid).adItemCrafted(ModuleEnergy.phaseGen),
            aSolarGen = new AchCrSingle("solargen", 2, 2, ModuleEnergy.solarGen, aPhaseGen).adItemCrafted(ModuleEnergy.solarGen),
            aWindGen = new AchCrSingle("windgen", 4, 2, ModuleEnergy.windgenFan, aSolarGen).adItemCrafted(ModuleEnergy.windgenMain),
            aCrystal = new AchEvItemPickup("crystal", 2, 0, ModuleCrafting.crystalLow, null).setTrigger(new ItemStack(ModuleCrafting.crystalLow)),
            aTerminal = new AchCrSingle("terminal", -2, 2, ModuleTerminal.terminalInstaller, null).adItemCrafted(ModuleTerminal.terminalInstaller)
        });
    }
    
}
