package cn.academy.misc.achievements.aches;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.misc.achievements.conds.ConItemCrafted;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

/**
 * @author EAirPeter
 */
public abstract class AchEvItemCrafted extends ACAchievement {

	//AchCr
	
	protected AchEvItemCrafted(String id, int x, int y, Item display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	protected AchEvItemCrafted(String id, int x, int y, Block display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	protected AchEvItemCrafted(String id, int x, int y, ItemStack display, Achievement parent) {
		super(id, x, y, display, parent);
	}
	
	public final ACAchievement adItemCrafted(Block block) {
		return adItemCrafted(new ConItemCrafted(block));
	}
	public final ACAchievement adItemCrafted(Item item) {
		return adItemCrafted(new ConItemCrafted(item));
	}
	public final ACAchievement adItemCrafted(Block block, int meta) {
		return adItemCrafted(new ConItemCrafted(block, meta));
	}
	public final ACAchievement adItemCrafted(Item item, int meta) {
		return adItemCrafted(new ConItemCrafted(item, meta));
	}
	public final ACAchievement adItemCrafted(Block block, int meta, int amount) {
		return adItemCrafted(new ConItemCrafted(block, meta, amount));
	}
	public final ACAchievement adItemCrafted(Item item, int meta, int amount) {
		return adItemCrafted(new ConItemCrafted(item, meta, amount));
	}
	public abstract ACAchievement adItemCrafted(ConItemCrafted cit);
	public abstract boolean acItemCrafted(ItemCraftedEvent event);
}
