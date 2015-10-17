package cn.academy.misc.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.LevelChangeEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cn.academy.misc.tutorial.ACTutorial.ACTutorialDataPart;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.util.helper.PlayerData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

@Registrant
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
			return PlayerData.get(player).getPart(ACTutorialDataPart.class).allSaved[index];
		}
		
		public void pass(EntityPlayer player){
			ACTutorialDataPart data = PlayerData.get(player).getPart(ACTutorialDataPart.class);
			data.allSaved[index]=true;
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
			if(data.getLevel()>=this.level&&(this.skillType==null||data.getCategory().equals(skillType)))return true;
			return false;
		}
		
	}

	@RegEventHandler()
	public static class HandleEvent{
		static HashMap<String,ItemCondition> craftMap = new HashMap<String,ItemCondition>();
		static HashMap<String,ItemCondition> pickupMap = new HashMap<String,ItemCondition>();
		static HashMap<String,ItemCondition> smeltMap = new HashMap<String,ItemCondition>();
		static HashMap<MatterMaterial,ItemCondition> matterUnitMap = new HashMap<MatterMaterial,ItemCondition>();
		
		@SubscribeEvent
		public void onItemCrafted(ItemCraftedEvent e){
			Item i = e.crafting.getItem();
			if(craftMap.containsKey(i.getUnlocalizedName()))craftMap.get(i.getUnlocalizedName()).pass(e.player);
		}
		
		@SubscribeEvent
		public void onItemPickup(ItemPickupEvent e){
			Item i = e.pickedUp.getEntityItem().getItem();
			if(pickupMap.containsKey(i.getUnlocalizedName()))pickupMap.get(i.getUnlocalizedName()).pass(e.player);
		}
		
		@SubscribeEvent
		public void onItemSmelted(ItemSmeltedEvent e){
			Item i = e.smelting.getItem();
			if(smeltMap.containsKey(i.getUnlocalizedName()))smeltMap.get(i.getUnlocalizedName()).pass(e.player);
		}
		
		@SubscribeEvent
		public void onMatterUnitHarvest(MatterUnitHarvestEvent e){
			MatterMaterial m = e.mat;
			if(matterUnitMap.containsKey(m))matterUnitMap.get(m).pass(e.player);
		}
	}
	
	Condition(){
	}
	
	Condition addChildren(Condition...condition){
		children=condition;
		return this;
	}
	
	void addNeedSavingToTutorial(ACTutorial t){
		if(this.needSaveNBT){
			if(!t.savedConditions.contains(this)){
				this.index=t.savedConditions.size();
				t.savedConditions.add(this);
			}
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
				for(Condition c0 : children){
					if(!c0.exam(player)){
						return false;
					}
				}
				return true;
			}
		}.addChildren(c);
	}
	
	//=============================================================================
	
	public static Condition or(Condition...c) throws Exception{
		return new Condition() {
			
			@Override
			public boolean exam(EntityPlayer player) {
				// TODO Auto-generated method stub
				for(Condition c0 : children){
					if(c0.exam(player)){
						return true;
					}
				}
				return false;
			}
		}.addChildren(c);
	}
	
	//=============================================================================
	
	public static Condition[] itemsCrafted(Object...itemsOrBlocks) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Object IorB : itemsOrBlocks){
			Item item = null;
			//java sucks?
			if(IorB instanceof Item)item = (Item) IorB;
			else if(IorB instanceof Block)item = Item.getItemFromBlock((Block) IorB);
			else{
				throw new Exception("Not an Item or Block: "+IorB);
			}
			if(HandleEvent.craftMap.containsKey(item.getUnlocalizedName())){
				c.add(HandleEvent.craftMap.get(item.getUnlocalizedName()));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.craftMap.put(item.getUnlocalizedName(), c0);
			}
		}
		return c.toArray(new Condition[c.size()]);
	}
	
	public static Condition itemCrafted(Item item) throws Exception{
		ItemCondition c;
		if(HandleEvent.craftMap.containsKey(item.getUnlocalizedName())){
			c=HandleEvent.craftMap.get(item.getUnlocalizedName());
		}else{
			c=new ItemCondition();
			HandleEvent.craftMap.put(item.getUnlocalizedName(),c);
		}
		return c;
	}
	
	public static Condition itemCrafted(Block block) throws Exception{
		Item item = Item.getItemFromBlock(block);
		return itemCrafted(item);
	}
	
	//=============================================================================
	
	public static Condition[] itemsPickup(Object...itemsOrBlocks) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Object IorB : itemsOrBlocks){
			Item item = null;
			//java sucks?
			if(IorB instanceof Item)item = (Item) IorB;
			else if(IorB instanceof Block)item = Item.getItemFromBlock((Block) IorB);
			else{
				throw new Exception("Not an Item or Block: "+IorB);
			}
			if(HandleEvent.pickupMap.containsKey(item.getUnlocalizedName())){
				c.add(HandleEvent.pickupMap.get(item.getUnlocalizedName()));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.pickupMap.put(item.getUnlocalizedName(), c0);
			}
		}
		return c.toArray(new Condition[c.size()]);
	}
	
	public static Condition itemPickup(Item item) throws Exception{
		ItemCondition c;
		if(HandleEvent.pickupMap.containsKey(item.getUnlocalizedName())){
			c=HandleEvent.pickupMap.get(item.getUnlocalizedName());
		}else{
			c=new ItemCondition();
			HandleEvent.pickupMap.put(item.getUnlocalizedName(), c);
		}
		return c;
	}
	
	public static Condition itemPickup(Block block) throws Exception{
		Item item = Item.getItemFromBlock(block);
		return itemPickup(item);
	}
	//=============================================================================
	
	public static Condition[] itemsSmelted(Object...itemsOrBlocks) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Object IorB : itemsOrBlocks){
			Item item = null;
			//java sucks?
			if(IorB instanceof Item)item = (Item) IorB;
			else if(IorB instanceof Block)item = Item.getItemFromBlock((Block) IorB);
			else{
				throw new Exception("Not an Item or Block: "+IorB);
			}
			if(HandleEvent.smeltMap.containsKey(item.getUnlocalizedName())){
				c.add(HandleEvent.smeltMap.get(item.getUnlocalizedName()));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.smeltMap.put(item.getUnlocalizedName(), c0);
			}
		}
		return c.toArray(new Condition[c.size()]);
	}
	
	public static Condition itemSmelted(Item item) throws Exception{
		ItemCondition c;
		if(HandleEvent.smeltMap.containsKey(item.getUnlocalizedName())){
			c=HandleEvent.smeltMap.get(item.getUnlocalizedName());
		}else{
			c=new ItemCondition();
			HandleEvent.smeltMap.put(item.getUnlocalizedName(), c);
		}
		return c;
	}

	public static Condition itemSmelted(Block block) throws Exception{
		Item item = Item.getItemFromBlock(block);
		return itemSmelted(item);
	}
	//=============================================================================
	
	public static Condition abilityLevel(Category cat,int level) throws Exception{
		return new AbilityLevelCondition(cat,level);
	}

	//=============================================================================
	
	public static Condition harvestLiquid(MatterMaterial mat) throws Exception{
		ItemCondition c;
		if(HandleEvent.matterUnitMap.containsKey(mat)){
			c=HandleEvent.matterUnitMap.get(mat);
		}else{
			c=new ItemCondition();
			HandleEvent.matterUnitMap.put(mat, c);
		}
		return c;
	}

	//=============================================================================
	
	public static Condition onTutorial(ACTutorial t) throws Exception{
		return new Condition(){

			@Override
			public boolean exam(EntityPlayer player) {
				// TODO Auto-generated method stub
				return t.getIsLoad(player);
			}
			
		};
	}	
	
	public static Condition onTutorial(String tutorialID) throws Exception{
		ACTutorial t = ACTutorial.getTutorial(tutorialID);
		return onTutorial(t);
	}

}
