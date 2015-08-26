package cn.academy.misc.achievements.pages;

import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.CategoryManager;
import cn.academy.misc.achievements.ItemAchievement;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchEvLevelChange;
import cn.academy.misc.achievements.aches.AchEvSkillLearn;
import cn.academy.vanilla.meltdowner.CatMeltdowner;

/**
 * @author EAirPeter
 */
public final class PageCtMeltdowner extends PageCategory<CatMeltdowner> {

	private final ACAchievement aLv1;
	private final ACAchievement aSkill;
	private final ACAchievement aLv2;
	private final ACAchievement aLv3;
	private final ACAchievement aLv4;
	private final ACAchievement aLv5;

	public PageCtMeltdowner() {
		super((CatMeltdowner) CategoryManager.INSTANCE.getCategory("meltdowner"));
		add(new ACAchievement[] {
			aLv1 = new AchEvLevelChange(1, category.electronBomb, "lv1", 0, 0, null),
			aSkill = new AchEvSkillLearn(category, "skill", 2, 0, ItemAchievement.getStack(category.getIcon()), aLv1),
			aLv2 = new AchEvLevelChange(2, category.lightShield, "lv2", 2, 2, aSkill),
			aLv3 = new AchEvLevelChange(3, category.meltdowner, "lv3", 0, 2, aLv2),
			aLv4 = new AchEvLevelChange(4, category.jetEngine, "lv4", -2, 2, aLv3),
			aLv5 = new AchEvLevelChange(5, category.electronMissile, "lv5", -2, 0, aLv4),
		});
	}
	
}
