package cn.academy.misc.achievements.pages;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchAbility;
import cn.academy.misc.achievements.aches.AchBasic;
import cn.academy.misc.achievements.aches.AchEvLevelChange;
import cn.academy.misc.achievements.aches.AchEvSkillLearn;

/**
 * @author EAirPeter
 */
public final class PageCtElectroMaster extends PageCategory {

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
	private final ACAchievement aIronSand;
	private final ACAchievement aMineDetect;
	private final ACAchievement aThunderBolt;
	private final ACAchievement aRailgun;
	private final ACAchievement aThunderClap;

	public PageCtElectroMaster() {
		super(CategoryManager.INSTANCE.getCategory("electro_master"));
		add(new ACAchievement[] {
			aLv1 = new AchEvLevelChange(1, category, "lv1", 0, 0, ModuleAbility.developerNormal, null),
			aSkill = new AchEvSkillLearn(category, "skill", 0, -2, ModuleAbility.developerNormal, aLv1),
			aLv2 = new AchEvLevelChange(2, category, "lv2", 2, 0, ModuleAbility.developerNormal, aSkill),
			aLv3 = new AchEvLevelChange(3, category, "lv3", 4, 0, ModuleAbility.developerNormal, aLv2),
			aLv4 = new AchEvLevelChange(4, category, "lv4", 6, 0, ModuleAbility.developerNormal, aLv3),
			aLv5 = new AchEvLevelChange(5, category, "lv5", 8, 0, ModuleAbility.developerNormal, aLv4),
		});
		add(new ACAchievement[] {
			aArcGen = new AchAbility(category, "arc_gen", 0, 2, ModuleAbility.developerNormal, null),
			aAtCreeper = new AchAbility(category, "attack_creeper", 2, 2, ModuleAbility.developerNormal, null),
			aMagnetic = new AchAbility(category, "magnetic", 4, 2, ModuleAbility.developerNormal, null),
			aBodyIntensify = new AchAbility(category, "body_intensify", 6, 2, ModuleAbility.developerNormal, null),
			aIronSand = new AchAbility(category, "iron_sand", 8, 2, ModuleAbility.developerNormal, null),
			aMineDetect = new AchAbility(category, "mine_detect", 0, 4, ModuleAbility.developerNormal, null),
			aThunderBolt = new AchAbility(category, "thunder_bolt", 2, 4, ModuleAbility.developerNormal, null),
			aRailgun = new AchAbility(category, "railgun", 4, 4, ModuleAbility.developerNormal, null),
			aThunderClap = new AchAbility(category, "thunder_clap", 6, 4, ModuleAbility.developerNormal, null),
		});
	}
	
}
