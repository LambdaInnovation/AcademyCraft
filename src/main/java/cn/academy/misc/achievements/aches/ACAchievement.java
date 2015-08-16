package cn.academy.misc.achievements.aches;

import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cn.academy.misc.achievements.AchDispatcher;
import cn.academy.misc.achievements.conds.ConItemCrafted;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

public abstract class ACAchievement extends Achievement {

	public ACAchievement(String id, int x, int y, Item display, Achievement parent) {
		this(id, x, y, new ItemStack(display), parent);
	}
	public ACAchievement(String id, int x, int y, Block display, Achievement parent) {
		this(id, x, y, new ItemStack(display), parent);
	}
	public ACAchievement(String id, int x, int y, ItemStack display, Achievement parent) {
		super("achievement.ac_" + id, "ac_" + id, x, y, display, parent);
		if (parent == null)
			this.initIndependentStat();
	}
	
	//ItemCraftedEvent
	public final ACAchievement rgItemCrafted(Block block) {
		return rgItemCrafted(new ConItemCrafted(block));
	}
	public final ACAchievement rgItemCrafted(Item item) {
		return rgItemCrafted(new ConItemCrafted(item));
	}
	public final ACAchievement rgItemCrafted(Block block, int meta) {
		return rgItemCrafted(new ConItemCrafted(block, meta));
	}
	public final ACAchievement rgItemCrafted(Item item, int meta) {
		return rgItemCrafted(new ConItemCrafted(item, meta));
	}
	public final ACAchievement rgItemCrafted(Block block, int meta, int amount) {
		return rgItemCrafted(new ConItemCrafted(block, meta, amount));
	}
	public final ACAchievement rgItemCrafted(Item item, int meta, int amount) {
		return rgItemCrafted(new ConItemCrafted(item, meta, amount));
	}
	public abstract ACAchievement rgItemCrafted(ConItemCrafted cit);
	public abstract void urItemCrafted();
	public abstract boolean acItemCrafted(ItemCraftedEvent event);
	
}
