package cn.academy.misc.achievements;

import java.util.HashMap;
import java.util.HashSet;

import cn.academy.misc.achievements.aches.ACAchievement;
import net.minecraft.item.Item;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

public class AchDispatcher {

	public static final AchDispatcher INSTANCE = new AchDispatcher();
	
	//ItemCraftedEvent
	private HashMap<Item, HashSet<ACAchievement>> hcItemCrafted = new HashMap<Item, HashSet<ACAchievement>>();
	public void rgItemCrafted(Item item, ACAchievement ach) {
		hcItemCrafted.getOrDefault(item, new HashSet<ACAchievement>()).add(ach);
	}
	public void urItemCrafted(Item item, ACAchievement ach) {
		HashSet<ACAchievement> set = hcItemCrafted.get(item);
		if (set != null)
			set.remove(ach);
	}
	@SubscribeEvent
	public void onItemCrafted(ItemCraftedEvent event) {
		HashSet<ACAchievement> set = hcItemCrafted.get(event.crafting.getItem());
		if (set != null)
			for (ACAchievement a : set)
				if (a.acItemCrafted(event))
					event.player.triggerAchievement(a);
	}
	
	
	
	private AchDispatcher() {
		FMLCommonHandler.instance().bus().register(this);
	}
	
	//stub method for loading
	public static void init() {
	}
	
}
