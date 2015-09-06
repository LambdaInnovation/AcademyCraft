package cn.academy.misc.achievements.pages;

import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.CategoryManager;
import cn.academy.misc.achievements.ItemAchievement;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchAbility;
import cn.academy.misc.achievements.aches.AchEvLevelChange;
import cn.academy.misc.achievements.aches.AchEvSkillLearn;
import cn.academy.vanilla.meltdowner.CatMeltdowner;

/**
 * @author EAirPeter
 */
public final class PageCtMeltdowner extends PageCategory<CatMeltdowner> {

	private final ACAchievement aLv1;
	private final ACAchievement aLv2;
	private final ACAchievement aLv3;
	private final ACAchievement aLv4;
	private final ACAchievement aLv5;

	private final ACAchievement aRadIntensify;
	private final ACAchievement aLightShield;
	private final ACAchievement aMeltdowner;
	private final ACAchievement aMineRay;
	private final ACAchievement aJetEngine;				//Manual
	private final ACAchievement aElectronMissile;
	
	public PageCtMeltdowner() {
		super((CatMeltdowner) CategoryManager.INSTANCE.getCategory("meltdowner"));
		add(new ACAchievement[] {
			aLv1 = new AchEvLevelChange(1, category.electronBomb, "lv1", 0, 0, null),
			aLv2 = new AchEvLevelChange(2, category.lightShield, "lv2", 2, 0, aLv1),
			aLv3 = new AchEvLevelChange(3, category.meltdowner, "lv3", 2, 2, aLv2),
			aLv4 = new AchEvLevelChange(4, category.jetEngine, "lv4", -2, 2, aLv3),
			aLv5 = new AchEvLevelChange(5, category.electronMissile, "lv5", -2, 0, aLv4),
		});
		add(new ACAchievement[] {
			aRadIntensify = new AchEvSkillLearn(category.radIntensify, -3, -1, null),
			aLightShield = new AchEvSkillLearn(category.lightShield, -3, 1, aRadIntensify),
			aMeltdowner = new AchEvSkillLearn(category.meltdowner, -3, 3, aLightShield),
			aMineRay = new AchEvSkillLearn(category.mineRayBasic, "mine_ray", -1, 3, ItemAchievement.getStack(category.mineRayLuck.getHintIcon()), aMeltdowner),
			aJetEngine = new AchAbility(category.jetEngine, 1, 3, aMineRay),
			aElectronMissile = new AchEvSkillLearn(category.electronMissile, 3, 3, aJetEngine),
		});
	}

}
