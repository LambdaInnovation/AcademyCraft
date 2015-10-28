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
import net.minecraft.item.ItemStack;

//@Registrant
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
		
		static String getKeyFromItemStack(ItemStack i){
			String key=i.getUnlocalizedName();
			if(i.getItem().getHasSubtypes())
				key+=i.getItemDamage();
			return key;
		}
		
		@SubscribeEvent
		public void onItemCrafted(ItemCraftedEvent e){
			String key=getKeyFromItemStack(e.crafting);
			if(craftMap.containsKey(key))craftMap.get(key).pass(e.player);
		}
		
		@SubscribeEvent
		public void onItemPickup(ItemPickupEvent e){
			String key=getKeyFromItemStack(e.pickedUp.getEntityItem());
			if(pickupMap.containsKey(key))pickupMap.get(key).pass(e.player);
		}
		
		@SubscribeEvent
		public void onItemSmelted(ItemSmeltedEvent e){
			String key=getKeyFromItemStack(e.smelting);
			if(smeltMap.containsKey(key))smeltMap.get(key).pass(e.player);
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
	

	static String getKeyFromItem(Item i,int subID){
		String key=i.getUnlocalizedName();
		if(i.getHasSubtypes())
			key+=subID;
		return key;
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
	/**
	 * 仅限传入的Item和Block不需要区分damageID或者meta或者对应的值是0的时候可以用这个来简tou化lan
	 * @param itemsOrBlocks
	 * @return
	 * @throws Exception
	 */
	public static Condition[] itemsCrafted(Object...itemsOrBlocks) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Object IorB : itemsOrBlocks){
			Item item = null;
			//java sucks?
			if(IorB instanceof Item)
				item = (Item) IorB;
			else if(IorB instanceof Block)
				item = Item.getItemFromBlock((Block) IorB);
			else{
				throw new Exception("Not an Item or Block: "+IorB);
			}
			String key=getKeyFromItem(item, 0);
			if(HandleEvent.craftMap.containsKey(key)){
				c.add(HandleEvent.craftMap.get(key));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.craftMap.put(key, c0);
			}
		}
		return c.toArray(new Condition[c.size()]);
	}
	
	public static Condition itemCrafted(Item item,int damageID) throws Exception{
		ItemCondition c;
		String key=getKeyFromItem(item, damageID);
		if(HandleEvent.craftMap.containsKey(key)){
			c=HandleEvent.craftMap.get(key);
		}else{
			c=new ItemCondition();
			HandleEvent.craftMap.put(key,c);
		}
		return c;
	}
	
	public static Condition itemCrafted(Item item) throws Exception{
		return itemCrafted(item, 0);
	}
	
	public static Condition itemCrafted(Block block,int meta) throws Exception{
		Item item = Item.getItemFromBlock(block);
		return itemCrafted(item,meta);
	}
	
	public static Condition itemCrafted(Block block) throws Exception{
		return itemCrafted(block, 0);
	}
	
	//=============================================================================
	/**
	 * 仅限传入的Item和Block不需要区分damageID或者meta或者对应的值是0的时候可以用这个来简tou化lan
	 * @param itemsOrBlocks
	 * @return
	 * @throws Exception
	 */
	public static Condition[] itemsPickup(Object...itemsOrBlocks) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Object IorB : itemsOrBlocks){
			Item item = null;
			//java sucks?
			if(IorB instanceof Item)
				item = (Item) IorB;
			else if(IorB instanceof Block)
				item = Item.getItemFromBlock((Block) IorB);
			else{
				throw new Exception("Not an Item or Block: "+IorB);
			}
			String key=getKeyFromItem(item, 0);
			if(HandleEvent.pickupMap.containsKey(key)){
				c.add(HandleEvent.pickupMap.get(key));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.pickupMap.put(key, c0);
			}
		}
		return c.toArray(new Condition[c.size()]);
	}
	
	public static Condition itemPickup(Item item,int damageID) throws Exception{
		ItemCondition c;
		String key=getKeyFromItem(item, damageID);
		if(HandleEvent.pickupMap.containsKey(key)){
			c=HandleEvent.pickupMap.get(key);
		}else{
			c=new ItemCondition();
			HandleEvent.pickupMap.put(key, c);
		}
		return c;
	}
	
	public static Condition itemPickup(Item item) throws Exception{
		return itemPickup(item, 0);
	}
	
	public static Condition itemPickup(Block block,int meta) throws Exception{
		Item item = Item.getItemFromBlock(block);
		return itemPickup(item,meta);
	}
	
	public static Condition itemPickup(Block block) throws Exception{
		return itemPickup(block,0);
	}
	//=============================================================================
	/**
	 * 仅限传入的Item和Block不需要区分damageID或者meta或者对应的值是0的时候可以用这个来简tou化lan
	 * @param itemsOrBlocks
	 * @return
	 * @throws Exception
	 */
	public static Condition[] itemsSmelted(Object...itemsOrBlocks) throws Exception{
		List<Condition> c=new ArrayList<Condition>();
		for(Object IorB : itemsOrBlocks){
			Item item = null;
			//java sucks?
			if(IorB instanceof Item)
				item = (Item) IorB;
			else if(IorB instanceof Block)
				item = Item.getItemFromBlock((Block) IorB);
			else{
				throw new Exception("Not an Item or Block: "+IorB);
			}
			String key=getKeyFromItem(item, 0);
			if(HandleEvent.smeltMap.containsKey(key)){
				c.add(HandleEvent.smeltMap.get(key));
			}else{
				ItemCondition c0=new ItemCondition();
				c.add(c0);
				HandleEvent.smeltMap.put(key, c0);
			}
		}
		return c.toArray(new Condition[c.size()]);
	}
	
	public static Condition itemSmelted(Item item,int damageID) throws Exception{
		ItemCondition c;
		String key=getKeyFromItem(item, damageID);
		if(HandleEvent.smeltMap.containsKey(key)){
			c=HandleEvent.smeltMap.get(key);
		}else{
			c=new ItemCondition();
			HandleEvent.smeltMap.put(key, c);
		}
		return c;
	}
	
	public static Condition itemSmelted(Item item) throws Exception{
		return itemSmelted(item, 0);
	}

	public static Condition itemSmelted(Block block,int meta) throws Exception{
		Item item = Item.getItemFromBlock(block);
		return itemSmelted(item,meta);
	}
	
	public static Condition itemSmelted(Block block) throws Exception{
		return itemSmelted(block, 0);
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
