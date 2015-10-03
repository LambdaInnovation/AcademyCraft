package cn.academy.misc.tutorial;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.util.helper.PlayerData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
@Registrant
@RegEventHandler(Bus.Forge)
public class ACTutorialUtils{
	private static Multimap<Class<? extends Item>,ACTutorial> updateOnCraftingMap=ArrayListMultimap.create();
	private static Multimap<Class<? extends Item>,ACTutorial> updateOnPickingMap=ArrayListMultimap.create();
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface RegTutorial{}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RegTutorialSet{}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface LoadOn{
		public enum Type{
			PICK,
			CRAFT;
		}
		
		Type condition();
		Class itemClass();
	}
	
	public static final int TUTORIAL_NUM=ACTutorialList.list.size();
	
	public static boolean isLoaded(EntityPlayer p, ACTutorial tutorial){
		ACTutorialDataPart data=PlayerData.get(p).getPart(ACTutorialDataPart.class);
		return data.getTutorialStatus(tutorial);
	}
	
	public static void updateStatus(EntityPlayer p, ACTutorial tutorial, boolean status){
		ACTutorialDataPart data=PlayerData.get(p).getPart(ACTutorialDataPart.class);
		data.updateTutorialStatus(tutorial, status);
	}
	
	public static String getTextByKey(String key){
		return ACTutorial.getTextByKey(key);
	}
	
	public static String getText(ACTutorial tutorial){
		return tutorial.getText();
	}
	
	
	public static void registTutorialUpdateOnItemCraft(ACTutorial tutorial, Item item){
		updateOnCraftingMap.put(item.getClass(), tutorial);
	}
	
	public static void registTutorialUpdateOnItemPick(ACTutorial tutorial, Item item){
		updateOnPickingMap.put(item.getClass(), tutorial);
	}
	
	
	@SubscribeEvent
	public void onItemCraft(ItemCraftedEvent event){
		for(ACTutorial tutorial : updateOnCraftingMap.get(event.crafting.getItem().getClass())){
			if(!isLoaded(event.player,tutorial))updateStatus(event.player,tutorial,true);
		}
	}
	
	@SubscribeEvent
	public void onItemPick(ItemPickupEvent event){
		for(ACTutorial tutorial : updateOnCraftingMap.get(event.pickedUp.getEntityItem().getItem().getClass())){
			if(!isLoaded(event.player,tutorial))updateStatus(event.player,tutorial,true);
		}
	}
}
