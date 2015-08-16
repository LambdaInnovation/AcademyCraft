package cn.academy.misc.achievements.aches;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.misc.achievements.conds.ConItemCrafted;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

public class AchBasic extends ACAchievement {

	public AchBasic(String id, int x, int y, Item display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	public AchBasic(String id, int x, int y, Block display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	public AchBasic(String id, int x, int y, ItemStack display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	
	@Override
	public ACAchievement rgItemCrafted(ConItemCrafted cit) {
		return this;
	}

	@Override
	public void urItemCrafted() {
	}

	@Override
	public boolean acItemCrafted(ItemCraftedEvent event) {
		return false;
	}

}
