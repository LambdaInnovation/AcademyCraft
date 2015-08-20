package cn.academy.misc.achievements.aches;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.ability.api.Category;
import cn.academy.misc.achievements.DispatcherAch;

/**
 * @author EAirPeter
 */
public final class AchEvSkillLearn extends AchAbility {

	public AchEvSkillLearn(Category cat, String id, int x, int y, Item display, Achievement parent) {
		super(cat, id, x, y, display, parent);
	}
	public AchEvSkillLearn(Category cat, String id, int x, int y, Block display, Achievement parent) {
		super(cat, id, x, y, display, parent);
	}
	public AchEvSkillLearn(Category cat, String id, int x, int y, ItemStack display, Achievement parent) {
		super(cat, id, x, y, display, parent);
	}
	
	@Override
	public void registerAll() {
		DispatcherAch.INSTANCE.rgSkillLearn(category, this);
	}

	@Override
	public void unregisterAll() {
		DispatcherAch.INSTANCE.urSkillLearn(category);
	}

}
