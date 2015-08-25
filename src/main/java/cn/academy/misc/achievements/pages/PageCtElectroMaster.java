package cn.academy.misc.achievements.pages;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.misc.achievements.ItemAchievement;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchAbility;
import cn.academy.misc.achievements.aches.AchBasic;
import cn.academy.misc.achievements.aches.AchEvLevelChange;
import cn.academy.misc.achievements.aches.AchEvSkillLearn;
import cn.academy.vanilla.electromaster.CatElectromaster;

/**
 * @author EAirPeter
 */
public final class PageCtElectromaster extends PageCategory<CatElectromaster> {

	private final ACAchievement aLv1;
	private final ACAchievement aSkill;
	private final ACAchievement aLv2;
	private final ACAchievement aLv3;
	private final ACAchievement aLv4;
	private final ACAchievement aLv5;
	
	private final ACAchievement aArcGen;
	private final ACAchievement aAtCreeper;
	private final ACAchievement aMagnetic;
	private final ACAchievement aBodyIntensify;
	//private final ACAchievement aIronSand;
	private final ACAchievement aMineDetect;
	private final ACAchievement aThunderBolt;
	private final ACAchievement aRailgun;
	private final ACAchievement aThunderClap;

	public PageCtElectromaster() {
		super((CatElectromaster) CategoryManager.INSTANCE.getCategory("electromaster"));
		add(new ACAchievement[] {
			aLv1 = new AchEvLevelChange(1, category.currentCharging, "lv1", 0, 0, null),
			aSkill = new AchEvSkillLearn(category, "skill", 2, 0, ItemAchievement.getStack(category.getIcon()), aLv1),
			aLv2 = new AchEvLevelChange(2, category.magManip, "lv2", 2, 2, aSkill),
			aLv3 = new AchEvLevelChange(3, category.bodyIntensify, "lv3", 0, 2, aLv2),
			aLv4 = new AchEvLevelChange(4, category.railgun, "lv4", -2, 2, aLv3),
			aLv5 = new AchEvLevelChange(5, category.thunderClap, "lv5", -2, 0, aLv4),
		});
		add(new ACAchievement[] {
			aArcGen = new AchAbility(category.arcGen, -3, -1, null),
			aAtCreeper = new AchAbility(category.thunderBolt, "attack_creeper", -3, 1, aArcGen),
			aMagnetic = new AchAbility(category.magMovement, -3, 3, aAtCreeper),
			aBodyIntensify = new AchAbility(category.bodyIntensify, -1, 3, aMagnetic),
			//aIronSand = new AchAbility(category.ironSand, 1, 3, null),
			aMineDetect = new AchAbility(category.mineDetect, 3, 3, aBodyIntensify),
			aThunderBolt = new AchAbility(category.thunderBolt, 3, 1, aMineDetect),
			aRailgun = new AchAbility(category.railgun, 3, -1, aThunderBolt),
			aThunderClap = new AchAbility(category.thunderClap, 1, -1, aRailgun),
		});
	}
	
}
