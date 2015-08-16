package cn.academy.misc.achievements.aches;

import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cn.academy.misc.achievements.AchDispatcher;
import cn.academy.misc.achievements.conds.ConItemCrafted;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

public class AchCSingle extends ACAchievement {

	public AchCSingle(String id, int x, int y, Item display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	public AchCSingle(String id, int x, int y, Block display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	public AchCSingle(String id, int x, int y, ItemStack display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	
	private ConItemCrafted cIT = null;
	
	@Override
	public AchCSingle rgItemCrafted(ConItemCrafted cit) {
		cIT = cit;
		AchDispatcher.INSTANCE.rgItemCrafted(cIT.item, this);
		return this;
	}

	@Override
	public void urItemCrafted() {
		AchDispatcher.INSTANCE.urItemCrafted(cIT.item, this);
		cIT = null;
	}

	@Override
	public boolean acItemCrafted(ItemCraftedEvent event) {
		return cIT.acItemStack(event.crafting);
	}

}
