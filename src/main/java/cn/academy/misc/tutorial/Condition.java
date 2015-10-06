package cn.academy.misc.tutorial;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.item.Item;

public abstract class Condition {
	ACTutorial tutorial;
	int index;
	int[] children;
	@Registrant
	@RegEventHandler(Bus.Forge)
	public static class HandleEvent{
		Multimap<Item,Condition> craftMap = ArrayListMultimap.create();
		@SubscribeEvent
		public void onItemCrafted(ItemCraftedEvent e){
			
		}
	}
	
	Condition(ACTutorial t){
		this.tutorial=t;
	}
	
	public Condition addChildren(Condition...condition){
		children=new int[condition.length];
		for(int i=0;i<condition.length;i++){
			children[i]=condition[i].index;
		}
		return this;
	}
	
	public abstract boolean exam();
	
	public static Condition and(String tutorial,Condition...c) throws Exception{
		return new Condition(ACTutorial.getTutorial(tutorial)) {
			
			@Override
			public boolean exam() {
				// TODO Auto-generated method stub
				for(int i : children){
					if(!this.tutorial.conditions.get(i)){
						return false;
					}
				}
				return true;
			}
		};
	}
	
	public static Condition or(String tutorial,Condition...c) throws Exception{
		return new Condition(ACTutorial.getTutorial(tutorial)) {
			
			@Override
			public boolean exam() {
				// TODO Auto-generated method stub
				for(int i : children){
					if(this.tutorial.conditions.get(i)){
						return true;
					}
				}
				return false;
			}
		};
	}
	
	public static Condition itemCrafted(String tutorial,Item item) throws Exception{
		
		return new Condition(ACTutorial.getTutorial(tutorial)) {
			boolean result=false;
			@Override
			public boolean exam() {
				// TODO Auto-generated method stub
				return result;
			}
		};
	}
}
