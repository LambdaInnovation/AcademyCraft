package cn.academy.misc.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cn.academy.misc.tutorial.ACTutorial.ACTutorialDataPart;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.util.datapart.EntityData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;

@Registrant
public abstract class Condition {
	int index;
	Condition[] children;
	boolean needSaveNBT=false;
	
	static class ItemCondition extends Condition {
		ItemCondition() {
			super();
			this.needSaveNBT=true;
		}

		@Override
		public boolean exam(EntityPlayer player) {
			return EntityData.get(player).getPart(ACTutorialDataPart.class).allSaved[index];
		}
		
		public void pass(EntityPlayer player) {
			ACTutorialDataPart data = EntityData.get(player).getPart(ACTutorialDataPart.class);
			data.allSaved[index] = true;
			data.update();
		}
		
	}

	static class AbilityLevelCondition extends Condition {
		int level;
		Category skillType;
		
		public AbilityLevelCondition(Category skillType,int level) {
			this.skillType=skillType;
			this.level=level;
		}
		
		@Override
		public boolean exam(EntityPlayer player) {
			AbilityData data=AbilityData.get(player);
			if(data.getLevel()>=this.level&&(this.skillType==null||data.getCategory().equals(skillType)))return true;
			return false;
		}
		
	}

	@RegEventHandler()
	public static class HandleEvent {
		static HashMap<String,ItemCondition> craftMap = new HashMap<String,ItemCondition>();
		static HashMap<String,ItemCondition> pickupMap = new HashMap<String,ItemCondition>();
		static HashMap<String,ItemCondition> smeltMap = new HashMap<String,ItemCondition>();
		static HashMap<MatterMaterial,ItemCondition> matterUnitMap = new HashMap<MatterMaterial,ItemCondition>();
		
		@SubscribeEvent
		public void onItemCrafted(ItemCraftedEvent e) {
			String key=e.crafting.getItem().getUnlocalizedName();
			try{
				craftMap.get(key).pass(e.player);
			} catch(NullPointerException err) {}
			try{
				if(e.crafting.getItem().getHasSubtypes()){
					key+=e.crafting.getItemDamage();
					craftMap.get(key).pass(e.player);
				}
			} catch(NullPointerException err) {}
		}
		
		@SubscribeEvent
		public void onItemPickup(ItemPickupEvent e) {
			String key=e.pickedUp.getEntityItem().getItem().getUnlocalizedName();
			try{
				pickupMap.get(key).pass(e.player);
			} catch(NullPointerException err) {}
			try {
				if(e.pickedUp.getEntityItem().getItem().getHasSubtypes()){
					key+=e.pickedUp.getEntityItem().getItemDamage();
					pickupMap.get(key).pass(e.player);
				}
			} catch (NullPointerException err) {}
		}
		
		@SubscribeEvent
		public void onItemSmelted(ItemSmeltedEvent e) {
			String key=e.smelting.getItem().getUnlocalizedName();
			try{
				smeltMap.get(key).pass(e.player);
			} catch(NullPointerException err) {}
			try{
				if(e.smelting.getItem().getHasSubtypes()){
					key+=e.smelting.getItemDamage();
					smeltMap.get(key).pass(e.player);
				}
			} catch(NullPointerException err) {}
		}
		
		@SubscribeEvent
		public void onMatterUnitHarvest(MatterUnitHarvestEvent e) {
			MatterMaterial m = e.mat;
			if(matterUnitMap.containsKey(m))matterUnitMap.get(m).pass(e.player);
		}
	}
	
	Condition() {}
	
	Condition addChildren(Condition...condition) {
		children=condition;
		return this;
	}
	
	void addNeedSavingToTutorial(ACTutorial t) {
		if(this.needSaveNBT){
			if(!ACTutorial.savedConditions.contains(this)){
				this.index=ACTutorial.savedConditions.size();
				ACTutorial.savedConditions.add(this);
			}
		}
		if(this.children!=null){
			for(Condition c : this.children)
				c.addNeedSavingToTutorial(t);
		}
	}
	
	public abstract boolean exam(EntityPlayer player);
	
	Condition setSaveToNBT() {
		this.needSaveNBT=true;
		return this;
	}
	

	static String getKeyFromItem(Item i,boolean checkID, int subID) {
		String key=i.getUnlocalizedName();
		if(i.getHasSubtypes()&&checkID)
			key+=subID;
		return key;
	}
	
	//=============================================================================
	/**
	 * AND
	 * @param c children condition
	 * @return result
	 */
	public static Condition and(Condition...c) {
		return new Condition() {
			
			@Override
			public boolean exam(EntityPlayer player) {
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
	/**
	 * OR
	 * @param c children condition
	 * @return result
	 */
	public static Condition or(Condition...c) {
		return new Condition() {
			
			@Override
			public boolean exam(EntityPlayer player) {
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
	 * 批量的物品/方块合成条件
	 * 仅限传入的Item和Block不需要区分damageID或者meta的时候可以用这个来简tou化lan
	 * @param itemsOrBlocks 物品/方块
	 * @return 一组条件分别对应每个物品/方块
	 * @throws Exception 传入的参数不是方块或者物品类型
	 */
	public static Condition[] itemsCrafted(Object...itemsOrBlocks) throws Exception {
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
			String key=getKeyFromItem(item,false,0);
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
	
	private static Condition itemCrafted(Item item,boolean checkID,int damageID) {
		ItemCondition c;
		String key=getKeyFromItem(item,checkID,damageID);
		if(HandleEvent.craftMap.containsKey(key)){
			c=HandleEvent.craftMap.get(key);
		}else{
			c=new ItemCondition();
			HandleEvent.craftMap.put(key,c);
		}
		return c;
	}
	
	public static Condition itemCrafted(Item item,int id) {
		return itemCrafted(item,true,id);
	}
	public static Condition itemCrafted(Item item) {
		return itemCrafted(item,false,0);
	}
	
	private static Condition itemCrafted(Block block,boolean checkMeta,int meta) {
		Item item = Item.getItemFromBlock(block);
		return itemCrafted(item,checkMeta,meta);
	}
	
	public static Condition itemCrafted(Block block,int meta) {
		return itemCrafted(block,true,meta);
	}
	public static Condition itemCrafted(Block block) {
		return itemCrafted(block,false,0);
	}
	
	//=============================================================================
	/**
	 * 批量的物品/方块捡起的条件
	 * 仅限传入的Item和Block不需要区分damageID或者meta或者对应的值是0的时候可以用这个来简tou化lan
	 * @param itemsOrBlocks 物品/方块
	 * @return 一组条件分别对应每个物品/方块
	 * @throws Exception 传入的参数不是方块或者物品类型
	 */
	public static Condition[] itemsPickup(Object...itemsOrBlocks) throws Exception {
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
			String key=getKeyFromItem(item,false,0);
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
	
	private static Condition itemPickup(Item item,boolean checkID,int damageID) {
		ItemCondition c;
		String key=getKeyFromItem(item,checkID,damageID);
		if(HandleEvent.pickupMap.containsKey(key)){
			c=HandleEvent.pickupMap.get(key);
		}else{
			c=new ItemCondition();
			HandleEvent.pickupMap.put(key, c);
		}
		return c;
	}
	
	public static Condition itemPickup(Item item,int id) {
		return itemPickup(item,true,id);
	}
	public static Condition itemPickup(Item item) {
		return itemPickup(item,false,0);
	}
	
	private static Condition itemPickup(Block block,boolean checkMeta,int meta) {
		Item item = Item.getItemFromBlock(block);
		return itemPickup(item,checkMeta,meta);
	}
	
	public static Condition itemPickup(Block block,int meta) {
		return itemPickup(block,true,meta);
	}
	public static Condition itemPickup(Block block) {
		return itemPickup(block,false,0);
	}
	//=============================================================================
	/**
	 * 批量的物品/方块烧制条件
	 * 仅限传入的Item和Block不需要区分damageID或者meta的时候可以用这个来简tou化lan
	 * @param itemsOrBlocks 物品/方块
	 * @return 一组条件分别对应每个物品/方块
	 * @throws Exception 传入的参数不是方块或者物品类型
	 */
	public static Condition[] itemsSmelted(Object...itemsOrBlocks) throws Exception {
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
			String key=getKeyFromItem(item,false,0);
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
	
	private static Condition itemSmelted(Item item,boolean checkID,int damageID) {
		ItemCondition c;
		String key=getKeyFromItem(item,checkID,damageID);
		if(HandleEvent.smeltMap.containsKey(key)){
			c=HandleEvent.smeltMap.get(key);
		}else{
			c=new ItemCondition();
			HandleEvent.smeltMap.put(key, c);
		}
		return c;
	}
	
	public static Condition itemSmelted(Item item,int id) {
		return itemSmelted(item,true,id);
	}
	public static Condition itemSmelted(Item item) {
		return itemSmelted(item,false,0);
	}

	private static Condition itemSmelted(Block block,boolean checkMeta,int meta) {
		Item item = Item.getItemFromBlock(block);
		return itemSmelted(item,checkMeta,meta);
	}
	
	public static Condition itemSmelted(Block block,int meta) {
		return itemSmelted(block,true,meta);
	}
	public static Condition itemSmelted(Block block) {
		return itemSmelted(block,false,0);
	}
	//=============================================================================
	
	public static Condition abilityLevel(Category cat,int level) {
		return new AbilityLevelCondition(cat,level);
	}

	//=============================================================================
	
	public static Condition harvestLiquid(MatterMaterial mat) {
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
	/**
	 * 
	 * @param t The tutorial
	 * @return result
	 */
	public static Condition onTutorial(ACTutorial t) {
		return new Condition(){

			@Override
			public boolean exam(EntityPlayer player) {
				return t.getIsLoad(player);
			}
			
		};
	}
	
	/**
	 * 
	 * @param tutorialID ID of the tutorial
	 * @return result
	 * @throws Exception Throw if no such a tutorial.
	 */
	public static Condition onTutorial(String tutorialID) throws Exception {
		ACTutorial t = ACTutorial.getTutorial(tutorialID);
		return onTutorial(t);
	}

}
