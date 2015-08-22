package cn.academy.misc.achievements.pages;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.misc.achievements.ItemAchievement;
import cn.academy.misc.achievements.aches.AchAbility;

/**
 * @author EAirPeter
 */
public abstract class PageCategory<Cat extends Category> extends ACAchievementPage{

	//PageCt
	
	protected final Cat category;
	
	public PageCategory(Cat cat) {
		super("cat_" + cat.getName());
		category = cat;
	}
	
	public final Category getCategory() {
		return category;
	}
	
}
