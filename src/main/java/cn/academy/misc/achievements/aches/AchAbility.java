package cn.academy.misc.achievements.aches;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.academy.ability.api.Category;
import cn.academy.misc.achievements.conds.ConItemCrafted;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

/**
 * @author EAirPeter
 */
public class AchAbility extends ACAchievement {

	//Ach
	//AchEv
	
	protected final Category category;
	
	public AchAbility(Category cat, String id, int x, int y, Item display, Achievement parent) {
		super(cat.getName() + "." + id, x, y, display, parent);
		category = cat;
	}
	public AchAbility(Category cat, String id, int x, int y, Block display, Achievement parent) {
		super(cat.getName() + "." + id, x, y, display, parent);
		category = cat;
	}
	public AchAbility(Category cat, String id, int x, int y, ItemStack display, Achievement parent) {
		super(cat.getName() + "." + id, x, y, display, parent);
		category = cat;
	}
	
	@Override
	public void registerAll() {
	}
	
	@Override
	public void unregisterAll() {
	}
	
}
