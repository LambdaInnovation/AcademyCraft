package cn.academy.misc.achievements.aches;

import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cn.academy.misc.achievements.DispatcherAch;
import cn.academy.misc.achievements.conds.ConItemCrafted;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public abstract class ACAchievement extends Achievement {
	
	//Ach
	//AchEv
	
	public ACAchievement(String id, int x, int y, Item display, Achievement parent) {
		this(id, x, y, new ItemStack(display), parent);
	}
	public ACAchievement(String id, int x, int y, Block display, Achievement parent) {
		this(id, x, y, new ItemStack(display), parent);
	}
	public ACAchievement(String id, int x, int y, ItemStack display, Achievement parent) {
		super("achievement.ac_" + id, "ac_" + id, x, y, display, parent);
		if (parent == null)
			initIndependentStat();
		registerStat();
	}
	
	public abstract void registerAll();
	public abstract void unregisterAll();
	
}
