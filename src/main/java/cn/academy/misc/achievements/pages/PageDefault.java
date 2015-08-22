package cn.academy.misc.achievements.pages;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.energy.ModuleEnergy;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchCrSingle;
import cn.academy.misc.achievements.aches.AchEvMatterUnitHarvest;

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
	
	public PageDefault() {
		super("default");
		add(new ACAchievement[] {
			aPhaseLiquid = new AchEvMatterUnitHarvest("phase_liquid", 0, 0, ModuleCrafting.imagPhase, null, ModuleCrafting.imagPhase),
			aMatrix1 = new AchCrSingle("matrix1", 2, 0, ModuleEnergy.matrix, aPhaseLiquid).adItemCrafted(ModuleEnergy.matrix),
			aMatrix2 = new AchCrSingle("matrix2", 4, 0, ModuleEnergy.matrixCore, aMatrix1).adItemCrafted(ModuleEnergy.matrixCore),
			aNode = new AchCrSingle("node", 2, 2, ModuleEnergy.nodeBasic, aPhaseLiquid).adItemCrafted(ModuleEnergy.nodeBasic),
			aDeveloper1 = new AchCrSingle("developer1", 4, 2, ModuleAbility.developerPortable, aNode).adItemCrafted(ModuleAbility.developerPortable),
			aDeveloper2 = new AchCrSingle("developer2", 6, 2, ModuleAbility.developerNormal, aDeveloper1).adItemCrafted(ModuleAbility.developerNormal),
			aDeveloper3 = new AchCrSingle("developer3", 8, 2, ModuleAbility.developerAdvanced, aDeveloper2).adItemCrafted(ModuleAbility.developerAdvanced),
			aPhaseGen = new AchCrSingle("phasegen", 2, 4, ModuleEnergy.phaseGen, aPhaseLiquid).adItemCrafted(ModuleEnergy.phaseGen),
			aSolarGen = new AchCrSingle("solargen", 4, 4, ModuleEnergy.solarGen, aPhaseGen).adItemCrafted(ModuleEnergy.solarGen),
			aWindGen = new AchCrSingle("windgen", 6, 4, ModuleEnergy.windgenFan, aSolarGen).adItemCrafted(ModuleEnergy.windgenMain),
			aCrystal = new AchCrSingle("crystal", 0, 6, ModuleCrafting.crystalLow, null).adItemCrafted(ModuleCrafting.crystalLow),
		});
	}
	
}
