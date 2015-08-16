package cn.academy.misc.achievements;

import java.util.HashSet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.common.AchievementPage;
import cn.academy.misc.achievements.pages.ACAchievementPage;
import cn.academy.misc.achievements.pages.PageDefault;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegEventHandler.Bus;

@Registrant
@RegInit
public class ModuleAchievements {

	private static PageDefault pageDefault;
	//TODO Implement PageAbility
	private static ACAchievementPage pageAbility;
	
	public static void init() {
		AchDispatcher.init();
		AchievementPage.registerAchievementPage(pageDefault = new PageDefault());
		AchievementPage.registerAchievementPage(pageAbility = new ACAchievementPage("Ability"));
	}
	
}
