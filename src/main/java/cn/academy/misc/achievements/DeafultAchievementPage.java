/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.achievements;

import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.energy.ModuleEnergy;
import cn.academy.misc.achievements.ACAchievements.CraftedItem;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class DeafultAchievementPage extends AchievementPage {
	
	public static Achievement 
		phaseLiquid,
		nodeBasic;
	
	public static void init() {
		phaseLiquid = ACAchievements.createAchievement("phase_liquid", ModuleCrafting.imagPhase, 2, 2, null);
		nodeBasic = ACAchievements.createAchievement("node_basic", ModuleEnergy.nodeBasic, 2, 4, phaseLiquid);
		
		// Init activation conditions.
		ACAchievements.regCraftCondition(new CraftedItem(ModuleEnergy.nodeBasic), nodeBasic);
		
		AchievementPage.registerAchievementPage(new DeafultAchievementPage());
	}

	private DeafultAchievementPage() {
		super("AcademyCraft", new Achievement[] {
			phaseLiquid,
			nodeBasic
		});
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void onMatterUnitHarvest(MatterUnitHarvestEvent event) {
		if(event.mat.block == ModuleCrafting.imagPhase) {
			event.player.triggerAchievement(phaseLiquid);
		}
	}

}
