package cn.academy.misc.achievements;

import java.util.HashSet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.AchievementPage;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.pages.PageCtElectroMaster;
import cn.academy.misc.achievements.pages.PageCtMeltDowner;
import cn.academy.misc.achievements.pages.PageCtTeleporter;
import cn.academy.misc.achievements.pages.PageDefault;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegEventHandler.Bus;

/**
 * @author EAirPeter
 */
@Registrant
@RegInit
public final class ModuleAchievements {

	private static PageDefault pageDefault;
	private static PageCtElectroMaster pageCtElectroMaster;
	private static PageCtMeltDowner pageCtMeltDowner;
	private static PageCtTeleporter pageCtTeleporter;
	
	public static void init() {
		DispatcherAch.init();
		
		AchievementPage.registerAchievementPage(pageDefault = new PageDefault());
		
		AchievementPage.registerAchievementPage(pageCtElectroMaster = new PageCtElectroMaster());
		AchievementPage.registerAchievementPage(pageCtMeltDowner = new PageCtMeltDowner());
		AchievementPage.registerAchievementPage(pageCtTeleporter = new PageCtTeleporter());
	}
	
	/**
	 * Trigger an event
	 * @param player The player
	 * @param achname The name of the achievement
	 */
	public static void trigger(EntityPlayer player, String achid) {
		ACAchievement ach = ACAchievement.getById(achid);
		if (ach != null)
			player.triggerAchievement(ach);
	}
	
}
