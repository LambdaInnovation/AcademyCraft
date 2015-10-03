package cn.academy.misc.tutorial2;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.util.helper.PlayerData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.item.Item;
@Registrant
public abstract class Condition{
	public abstract boolean exam();
	public abstract void update(boolean b);

	public abstract static class ConditionToItem extends Condition{
		boolean result = false;
		public Item targetItem;
		static Multimap<Item,ConditionToItem> conditionMap =ArrayListMultimap.create();
		@Override
		public boolean exam() {
			// TODO Auto-generated method stub
			return result;
		}

		@Override
		public void update(boolean b) {
			// TODO Auto-generated method stub
			this.result = b;
		}
		
		public boolean equals(Object o){
			return this.getClass() == o.getClass() && this.toString().equals(o.toString());
		}
		
		public String toString(){
			return this.getClass().getSimpleName() + " target on Item: " + targetItem.getUnlocalizedName();
		}
		
		public static ConditionToItem targetOn(Item i){
			ConditionToItem c=null;
			Class clazz=new Object(){
				Class getClazz(){
					return this.getClass().getEnclosingClass();
				}
			}.getClazz();
			if(!conditionMap.containsKey(i)){
				try {
					c= (ConditionToItem) clazz.newInstance();
					c.targetItem=i;
					return c;
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				for(ConditionToItem c0 : conditionMap.get(i)){
					if(c0.getClass()==clazz)c=c0;
				}
			}
			return c;
		}
	}
	
	@RegEventHandler(Bus.Forge)
	public static class ConditionToItemPick extends ConditionToItem{
		
		@SubscribeEvent
		public void onPickup(ItemPickupEvent e){
			if(e.pickedUp.getEntityItem().getItem()==targetItem){
				ACTutorial.ACTutorialDataPart data = PlayerData.get(e.player).getPart(ACTutorial.ACTutorialDataPart.class);
				data.update(this,true);
			}
		}
	}
	
	@RegEventHandler(Bus.Forge)
	public static class ConditionToItemCrafted extends ConditionToItem{
		
		@SubscribeEvent
		public void onCrafted(ItemCraftedEvent e){
			if(e.crafting.getItem()==targetItem){
				ACTutorial.ACTutorialDataPart data = PlayerData.get(e.player).getPart(ACTutorial.ACTutorialDataPart.class);
				data.update(this,true);
			}
		}
	}
	
	@RegEventHandler(Bus.Forge)
	public static class ConditionToItemSmelted extends ConditionToItem{
		
		@SubscribeEvent
		public void onCrafted(ItemSmeltedEvent e){
			if(e.smelting.getItem()==targetItem){
				ACTutorial.ACTutorialDataPart data = PlayerData.get(e.player).getPart(ACTutorial.ACTutorialDataPart.class);
				data.update(this,true);
			}
		}
	}

	public static class ConditionToTutorial extends Condition{
		public ACTutorial targetTutorial;
		
		@Override
		public boolean exam() {
		// TODO Auto-generated method stub
			return targetTutorial.getIsLoad();
		}

		@Override
		public void update(boolean b) {}
	}
}