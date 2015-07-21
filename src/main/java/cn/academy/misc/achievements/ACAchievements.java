package cn.academy.misc.achievements;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

@Registrant
@RegEventHandler(Bus.FML)
public class ACAchievements {
	
	private static Map<CraftedItem, Achievement> craftConditions = new HashMap();
	
	public static Achievement createAchievement(String name, Block block, int row, int col, Achievement parent) {
		return createAchievement(name, new ItemStack(block), row, col, parent);
	}
	
	public static Achievement createAchievement(String name, Item item, int row, int col, Achievement parent) {
		return createAchievement(name, new ItemStack(item), row, col, parent);
	}
	
	public static Achievement createAchievement(String name, ItemStack stack, int row, int col, Achievement parent) {
		String id = "ac_" + name;
		return new Achievement("achievement" + id, id, row, col, stack, parent);
	}
	
	public static void regCraftCondition(CraftedItem item, Achievement ach) {
		craftConditions.put(item, ach);
	}
	
	public static class CraftedItem {
		final Item item;
		final int meta; //if meta==-1 then is not metadata sensitive
		
		public CraftedItem(Item _item, int _meta) {
			item = _item;
			meta = _meta;
		}
		
		public CraftedItem(Item item) {
			this(item, -1);
		}
		
		public CraftedItem(Block _block, int _meta) {
			this(Item.getItemFromBlock(_block), _meta);
		}
		
		public CraftedItem(Block block) {
			this(block, -1);
		}
		
		public boolean accepts(ItemStack stack) {
			return stack.getItem() == item && (meta == -1 || meta == stack.getItemDamage());
		}
	}
	
	@SubscribeEvent
	public void onItemCrafted(ItemCraftedEvent event) {
		for(Entry<CraftedItem, Achievement> ent : craftConditions.entrySet()) {
			if(ent.getKey().accepts(event.crafting)) {
				event.player.triggerAchievement(ent.getValue());
				break;
			}
		}
	}
	
}
