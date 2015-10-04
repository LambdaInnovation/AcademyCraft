package cn.academy.misc.tutorial2;

import java.util.HashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.item.Item;

public class ActivationHelper {
	static HashMap<Item,Condition> craftMap = new HashMap<Item,Condition>();
	static HashMap<Item,Condition> pickupMap = new HashMap<Item,Condition>();
	static HashMap<Item,Condition> smeltMap = new HashMap<Item,Condition>();
	
	private static class Condition{
		ACTutorial tutorial;
		int index;
		Condition(ACTutorial t){
			this.tutorial=t;
			this.index=t.andConditions.size();
		}
	}
	
	public static void andActivateItemCraft(String TutorialID,Item...items) throws Exception{
		int i = 0;
		for(Item item : items){
			ACTutorial.getTutorial(TutorialID).andConditions.add(Boolean.FALSE);
			craftMap.put(item, new Condition(ACTutorial.getTutorial(TutorialID)));
		}
	}
	
	public static void orActivateItemCraft(String TutorialID,Item...items) throws Exception{
		int i = 0;
		for(Item item : items){
			ACTutorial.getTutorial(TutorialID).orConditions.add(Boolean.FALSE);
			craftMap.put(item, new Condition(ACTutorial.getTutorial(TutorialID)));
		}
	}
	
	public static void andActivateItemPickup(String TutorialID,Item...items) throws Exception{
		int i = 0;
		for(Item item : items){
			ACTutorial.getTutorial(TutorialID).andConditions.add(Boolean.FALSE);
			pickupMap.put(item, new Condition(ACTutorial.getTutorial(TutorialID)));
		}
	}
	
	public static void orActivateItemPickup(String TutorialID,Item...items) throws Exception{
		int i = 0;
		for(Item item : items){
			ACTutorial.getTutorial(TutorialID).orConditions.add(Boolean.FALSE);
			pickupMap.put(item, new Condition(ACTutorial.getTutorial(TutorialID)));
		}
	}
	
	public static void andActivateItemSmelt(String TutorialID,Item...items) throws Exception{
		int i = 0;
		for(Item item : items){
			ACTutorial.getTutorial(TutorialID).andConditions.add(Boolean.FALSE);
			smeltMap.put(item, new Condition(ACTutorial.getTutorial(TutorialID)));
		}
	}
	
	public static void orActivateItemSmelt(String TutorialID,Item...items) throws Exception{
		int i = 0;
		for(Item item : items){
			ACTutorial.getTutorial(TutorialID).orConditions.add(Boolean.FALSE);
			smeltMap.put(item, new Condition(ACTutorial.getTutorial(TutorialID)));
		}
	}
	
//======================================================================================================================================
	
	@SubscribeEvent
	public void onItemCrafted(ItemCraftedEvent e){
		Item i=e.crafting.getItem();
		if(!craftMap.containsKey(i))return;
		Condition c = craftMap.get(i);
		c.tutorial.update(e.player,c.index);
	}
	
	@SubscribeEvent
	public void onItemPickup(ItemPickupEvent e){
		Item i=e.pickedUp.getEntityItem().getItem();
		if(!pickupMap.containsKey(i))return;
		Condition c = craftMap.get(i);
		c.tutorial.update(e.player,c.index);
	}
	
	@SubscribeEvent
	public void onItemSmelted(ItemSmeltedEvent e){
		Item i=e.smelting.getItem();
		if(!smeltMap.containsKey(i))return;
		Condition c = craftMap.get(i);
		c.tutorial.update(e.player,c.index);
	}
}
