package cn.academy.misc.tutorial;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.annoreg.base.RegistrationFieldSimple;
import cn.liutils.util.helper.PlayerData;
import net.minecraft.entity.player.EntityPlayer;

public class ACTutorialUtils{
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface RegTutorial{}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RegTutorialSet{}
	
	public static final int TUTORIAL_NUM=ACTutorialList.list.size();
	
	public static boolean isLoaded(EntityPlayer p,ACTutorial tutorial){
		ACTutorialDataPart data=PlayerData.get(p).getPart(ACTutorialDataPart.class);
		return data.getTutorialStatus(tutorial);
	}
	
	public static void updateStatus(EntityPlayer p,ACTutorial tutorial,boolean status){
		ACTutorialDataPart data=PlayerData.get(p).getPart(ACTutorialDataPart.class);
		data.updateTutorialStatus(tutorial, status);
	}
	
	public static String getTextByKey(String key){
		return ACTutorial.getTextByKey(key);
	}
	
	public static String getText(ACTutorial tutorial){
		return tutorial.getText();
	}
}
