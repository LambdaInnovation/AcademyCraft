package cn.academy.misc.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.LevelChangeEvent;
import cn.academy.misc.tutorial.ACTutorial.ACTutorialDataPart;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.util.helper.PlayerData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;

public abstract class Condition {
	int index;
	Condition[] children;
	boolean needSaveNBT=false;
	
	static class ItemCondition extends Condition{
		ItemCondition() {
			super();
			this.needSaveNBT=true;
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean exam(EntityPlayer player) {
			// TODO Auto-generated method stub
			return PlayerData.get(player).getPart(ACTutorialDataPart.class).allSaved.get(index);
		}
		
		public void pass(EntityPlayer player){
			ACTutorialDataPart data = PlayerData.get(player).getPart(ACTutorialDataPart.class);
			data.allSaved.set(index, Boolean.TRUE);
			data.update();
		}
	}

	static class AbilityLevelCondition extends Condition{
		int level;
		Category skillType;
		
		public AbilityLevelCondition(Category skillType,int level) {
			// TODO Auto-generated constructor stub
			this.skillType=skillType;
			this.level=level;
		}
		
		@Override
		public boolean exam(EntityPlayer player) {
			// TODO Auto-generated method stub
			AbilityData data=AbilityData.get(player);
			if(data.getLevel()>this.level&&(this.skillType==null||data.getCategory().equals(skillType)))return true;
			return false;
		}
		
	}
	
	@Registrant
	@RegEventHandler(Bus.Forge)
	static class HandleEvent{
		static HashMap<Item,ItemCondition> craftMap = new HashMap<Item,ItemCondition>();
		static HashMap<Item,ItemCondition> pickupMap = new HashMap<Item,ItemCondition>();
		static HashMap<Item,ItemCondition> smeltMap = new HashMap<Item,ItemCondition>();
		
		@SubscribeEvent
		public void onItemCrafted(ItemCraftedEvent e){
			Item i = e.crafting.getItem();
			if(craftMap.containsKey(i))craftMap.get(i).pass(e.player);;
		}
		
		@SubscribeEvent
		public void onItemPickup(ItemPickupEvent e){
			Item i = e.pickedUp.getEntityItem().getItem();
			if(craftMap.containsKey(i))craftMap.get(i).pass(e.player);;
		}
		
		@SubscribeEvent
		public void onItemSmelted(ItemSmeltedEvent e){
			Item i = e.smelting.getItem();
			if(craftMap.containsKey(i))craftMap.get(i).pass(e.player);;
		}
	}
	
	Condition(){
	}
	
	Condition addChildren(Condition...condition){
		children=condition;
		return this;
	}
	
	void addNeedSavingToTutorial(ACTutorial t){
		if(this.needSaveNBT)if(!t.savedConditions.contains(this)){
			this.index=t.savedConditions.size();
			t.savedConditions.add(this);
		}
		if(this.children!=null){
			for(Condition c : this.children)
				c.addNeedSavingToTutorial(t);
		}
	}
	
	public abstract boolean exam(EntityPlayer player);
	
	Condition setSaveToNBT(){
		this.needSaveNBT=true;
		return this;
	}
	
	//=============================================================================
	
	public static Condition and(Condition...c) throws Exception{
		return new Condition() {
			
			@Override
			public boolean exam(EntityPlayer player) {
				// TODO Auto-generated method stub
				for(Condition c : children){
					if(!c.exam(player)){
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
			public boolean exam(EntityPlayer player) {
				// TODO Auto-generated method stub
				for(Condition c : children){
					if(c.exam(player)){
						return true;
					}
				}
				return false;
			}
		};
	}
	
	//=============================================================================
	
	public static ItemCondition[] itemsCrafted(Item...items) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Item item : items){
			if(HandleEvent.craftMap.containsKey(item)){
				c.add(HandleEvent.craftMap.get(item));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.craftMap.put(item, c0);
			}
		}
		return (ItemCondition[])c.toArray();
	}
	
	public static ItemCondition itemCrafted(Item item) throws Exception{
		ItemCondition c;
		if(HandleEvent.craftMap.containsKey(item)){
			c=HandleEvent.craftMap.get(item);
		}else{
			c=new ItemCondition();
			HandleEvent.craftMap.put(item, c);
		}
		return c;
	}
	
	//=============================================================================
	
	public static ItemCondition[] itemsPickup(Item...items) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Item item : items){
			if(HandleEvent.pickupMap.containsKey(item)){
				c.add(HandleEvent.pickupMap.get(item));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.pickupMap.put(item, c0);
			}
		}
		return (ItemCondition[])c.toArray();
	}
	
	public static ItemCondition itemPickup(Item item) throws Exception{
		ItemCondition c;
		if(HandleEvent.pickupMap.containsKey(item)){
			c=HandleEvent.pickupMap.get(item);
		}else{
			c=new ItemCondition();
			HandleEvent.pickupMap.put(item, c);
		}
		return c;
	}
	
	//=============================================================================
	
	public static ItemCondition[] itemsSmelted(Item...items) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Item item : items){
			if(HandleEvent.smeltMap.containsKey(item)){
				c.add(HandleEvent.smeltMap.get(item));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.smeltMap.put(item, c0);
			}
		}
		return (ItemCondition[])c.toArray();
	}
	
	public static ItemCondition itemSmelted(Item item) throws Exception{
		ItemCondition c;
		if(HandleEvent.smeltMap.containsKey(item)){
			c=HandleEvent.smeltMap.get(item);
		}else{
			c=new ItemCondition();
			HandleEvent.smeltMap.put(item, c);
		}
		return c;
	}

	//=============================================================================
	
	public static Condition abilityLevel(Category cat,int level) throws Exception{
		return new AbilityLevelCondition(cat,level);
	}
	
}
