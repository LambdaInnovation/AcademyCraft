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
import cn.academy.vanilla.electromaster.CatElectroMaster;

/**
 * @author EAirPeter
 */
public final class PageCtElectroMaster extends PageCategory<CatElectroMaster> {

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

	public PageCtElectroMaster() {
		super((CatElectroMaster) CategoryManager.INSTANCE.getCategory("electro_master"));
		add(new ACAchievement[] {
			aLv1 = new AchEvLevelChange(1, category, "lv1", 0, 0, ModuleAbility.developerNormal, null),
			aSkill = new AchEvSkillLearn(category, "skill", 2, 0, ModuleAbility.developerNormal, aLv1),
			aLv2 = new AchEvLevelChange(2, category, "lv2", 2, 2, ModuleAbility.developerNormal, aSkill),
			aLv3 = new AchEvLevelChange(3, category, "lv3", 0, 2, ModuleAbility.developerNormal, aLv2),
			aLv4 = new AchEvLevelChange(4, category, "lv4", -2, 2, ModuleAbility.developerNormal, aLv3),
			aLv5 = new AchEvLevelChange(5, category, "lv5", -2, 0, ModuleAbility.developerNormal, aLv4),
		});
		add(new ACAchievement[] {
			aArcGen = new AchAbility(category.arcGen, "arc_gen", -3, -1, null),
			aAtCreeper = new AchAbility(category.thunderBolt, "attack_creeper", -3, 1, aArcGen),
			aMagnetic = new AchAbility(category.magMovement, "magnetic", -3, 3, aAtCreeper),
			aBodyIntensify = new AchAbility(category.bodyIntensify, "body_intensify", -1, 3, aMagnetic),
			//aIronSand = new AchAbility(category.ironSand, "iron_sand", 1, 3, null),
			aMineDetect = new AchAbility(category.mineDetect, "mine_detect", 3, 3, aBodyIntensify),
			aThunderBolt = new AchAbility(category.thunderBolt, "thunder_bolt", 3, 1, aMineDetect),
			aRailgun = new AchAbility(category.railgun, "railgun", 3, -1, aThunderBolt),
			aThunderClap = new AchAbility(category.thunderClap, "thunder_clap", 1, -1, aRailgun),
		});
	}
	
}
