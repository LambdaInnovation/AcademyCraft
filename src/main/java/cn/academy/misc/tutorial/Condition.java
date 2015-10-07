package cn.academy.misc.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.item.Item;

public abstract class Condition {
	int index;
	Condition[] children;
	boolean result=false;
	boolean needSaveNBT=false;
	
	static class itemCondition extends Condition{
		itemCondition() {
			super();
			this.needSaveNBT=true;
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean exam() {
			// TODO Auto-generated method stub
			return result;
		}
	}
	
	@Registrant
	@RegEventHandler(Bus.Forge)
	static class HandleEvent{
		static HashMap<Item,Condition> craftMap = new HashMap<Item,Condition>();
		static HashMap<Item,Condition> pickupMap = new HashMap<Item,Condition>();
		static HashMap<Item,Condition> smeltMap = new HashMap<Item,Condition>();
		
		@SubscribeEvent
		public void onItemCrafted(ItemCraftedEvent e){
			Item i = e.crafting.getItem();
			if(craftMap.containsKey(i))craftMap.get(i).result=true;
		}
		
		@SubscribeEvent
		public void onItemPickup(ItemPickupEvent e){
			Item i = e.pickedUp.getEntityItem().getItem();
			if(craftMap.containsKey(i))craftMap.get(i).result=true;
		}
		
		@SubscribeEvent
		public void onItemSmelted(ItemSmeltedEvent e){
			Item i = e.smelting.getItem();
			if(craftMap.containsKey(i))craftMap.get(i).result=true;
		}
	}
	
	Condition(){
	}
	
	Condition addChildren(Condition...condition){
		children=condition;
		return this;
	}
	
	void addAllNeedSavingChildrenToTutorial(ACTutorial t){
		for(Condition c : this.children){
			if(c.needSaveNBT){
				if(!t.savedConditions.contains(c))t.savedConditions.add(c);
			}
			if(c.children!=null){
				for(Condition cc : c.children)
					cc.addAllNeedSavingChildrenToTutorial(t);
			}
		}
	}
	
	public abstract boolean exam();
	
	Condition setSaveToNBT(){
		this.needSaveNBT=true;
		return this;
	}
	
	//=============================================================================
	
	public static Condition and(Condition...c) throws Exception{
		return new Condition() {
			
			@Override
			public boolean exam() {
				// TODO Auto-generated method stub
				for(Condition c : children){
					if(!c.exam()){
						return false;
					}
				}
				return true;
			}
		};
	}
	
	//=============================================================================
	
	public static Condition or(Condition...c) throws Exception{
		return new Condition() {
			
			@Override
			public boolean exam() {
				// TODO Auto-generated method stub
				for(Condition c : children){
					if(c.exam()){
						return true;
					}
				}
				return false;
			}
		};
	}
	
	//=============================================================================
	
	public static Condition[] itemsCrafted(Item...items) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Item item : items){
			if(HandleEvent.craftMap.containsKey(item)){
				c.add(HandleEvent.craftMap.get(item));
			}else{
				Condition c0=new itemCondition();
				c.add(c0);
				HandleEvent.craftMap.put(item, c0);
			}
		}
		return (Condition[])c.toArray();
	}
	
	public static Condition itemCrafted(Item item) throws Exception{
		Condition c;
		if(HandleEvent.craftMap.containsKey(item)){
			c=HandleEvent.craftMap.get(item);
		}else{
			c=new itemCondition();
			HandleEvent.craftMap.put(item, c);
		}
		return c;
	}
	
	//=============================================================================
	
	public static Condition[] itemsPickup(Item...items) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Item item : items){
			if(HandleEvent.pickupMap.containsKey(item)){
				c.add(HandleEvent.pickupMap.get(item));
			}else{
				Condition c0=new itemCondition();
				c.add(c0);
				HandleEvent.pickupMap.put(item, c0);
			}
		}
		return (Condition[])c.toArray();
	}
	
	public static Condition itemPickup(Item item) throws Exception{
		Condition c;
		if(HandleEvent.pickupMap.containsKey(item)){
			c=HandleEvent.pickupMap.get(item);
		}else{
			c=new itemCondition();
			HandleEvent.pickupMap.put(item, c);
		}
		return c;
	}
	
	//=============================================================================
	
	public static Condition[] itemsSmelted(Item...items) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Item item : items){
			if(HandleEvent.smeltMap.containsKey(item)){
				c.add(HandleEvent.smeltMap.get(item));
			}else{
				Condition c0=new itemCondition();
				c.add(c0);
				HandleEvent.smeltMap.put(item, c0);
			}
		}
		return (Condition[])c.toArray();
	}
	
	public static Condition itemSmelted(Item item) throws Exception{
		Condition c;
		if(HandleEvent.smeltMap.containsKey(item)){
			c=HandleEvent.smeltMap.get(item);
		}else{
			c=new itemCondition();
			HandleEvent.smeltMap.put(item, c);
		}
		return c;
	}
	
}
