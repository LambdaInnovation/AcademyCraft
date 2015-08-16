package cn.academy.misc.achievements.pages;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.ability.ModuleAbility;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.energy.ModuleEnergy;
import cn.academy.misc.achievements.aches.ACAchievement;

public class PageDefault extends ACAchievementPage {

	private ACAchievement aPhaseLiquid;
	private ACAchievement  aMatrix1;
	private ACAchievement   aMatrix2;
	private ACAchievement  aNode;
	private ACAchievement   aDeveloper1;
	private ACAchievement    aLv1;
	private ACAchievement     aSkill;
	private ACAchievement      aLv2;
	private ACAchievement       aLv3;
	private ACAchievement        aLv4;
	private ACAchievement         aLv5;
	private ACAchievement    aDeveloper2;
	private ACAchievement     aDeveloper3;
	private ACAchievement  aPhaseGen;
	private ACAchievement   aSolarGen;
	private ACAchievement    aWindGen;
	private ACAchievement aCrystal;
	
	public PageDefault() {
		super("AcademyCraft");
		aPhaseLiquid = addAchBasic("phaseLiquid", 0, 0, ModuleCrafting.imagPhase, null);
			aMatrix1 = addAchCSingle("matrix1", 2, 0, ModuleEnergy.matrix, aPhaseLiquid).rgItemCrafted(ModuleEnergy.matrix);
				aMatrix2 = addAchCSingle("matrix2", 4, 0, ModuleEnergy.matrixCore, aMatrix1).rgItemCrafted(ModuleEnergy.matrixCore);;
			aNode = addAchCSingle("node", 2, 2, ModuleEnergy.nodeBasic, aPhaseLiquid).rgItemCrafted(ModuleEnergy.nodeBasic);
				aDeveloper1 = addAchCSingle("developer1", 4, 2, ModuleAbility.developerPortable, aNode).rgItemCrafted(ModuleAbility.developerPortable);
					aLv1 = addAchBasic("lv1", 6, 2, ModuleAbility.developerNormal, aDeveloper1);
						aSkill = addAchBasic("skill", 8, 2, ModuleAbility.developerNormal, aLv1);
							aLv2 = addAchBasic("lv2", 10, 2, ModuleAbility.developerNormal, aSkill);
								aLv3 = addAchBasic("lv3", 12, 2, ModuleAbility.developerNormal, aLv2);
									aLv4 = addAchBasic("lv4", 14, 2, ModuleAbility.developerNormal, aLv3);
										aLv5 = addAchBasic("lv5", 16, 2, ModuleAbility.developerNormal, aLv4);
					aDeveloper2 = addAchCSingle("developer2", 6, 4, ModuleAbility.developerNormal, aDeveloper1).rgItemCrafted(ModuleAbility.developerNormal);
						aDeveloper3 = addAchCSingle("developer3", 8, 4, ModuleAbility.developerAdvanced, aDeveloper2).rgItemCrafted(ModuleAbility.developerAdvanced);
			aPhaseGen = addAchCSingle("phaseGen", 2, 6, ModuleEnergy.phaseGen, aPhaseLiquid).rgItemCrafted(ModuleEnergy.phaseGen);
				aSolarGen = addAchCSingle("solarGen", 4, 6, ModuleEnergy.solarGen, aPhaseGen).rgItemCrafted(ModuleEnergy.solarGen);
					aWindGen = addAchCSingle("windGen", 6, 6, ModuleEnergy.windgenFan, aSolarGen).rgItemCrafted(ModuleEnergy.windgenMain);
		aCrystal = addAchCSingle("crystal", 0, 8, ModuleCrafting.crystalLow, null).rgItemCrafted(ModuleCrafting.crystalLow);
		genList();
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void onMatterUnitHarvest(MatterUnitHarvestEvent event) {
		if (event.mat.block == ModuleCrafting.imagPhase)
			event.player.triggerAchievement(aPhaseLiquid);
	}
	
}
