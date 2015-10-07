package cn.academy.misc.tutorial;

import java.util.HashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import net.minecraft.item.Item;

public class ActivationHelper {
	public static void addConditions(String tutorialID,Condition...c) throws Exception{
		ACTutorial t=ACTutorial.getTutorial(tutorialID);
		t.addCondition(c);
	}
}
