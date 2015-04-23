/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.generic.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.IntHashMap;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.util.RegUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This class overrides(i.e.Disables) vanilla minecraft's control on a certain key. 
 * It is currently intented to be used DURING gameplay and will unlock all overrides when any GUI is present,
 * and restore 
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegistrationClass
@RegSubmoduleInit
@RegEventHandler(Bus.Forge)
public class ControlOverrider {
	
	private static IntHashMap kbMap;
	private static Field pressedField;
	
	private static Map<Integer, KeyBinding> activeOverrides = new HashMap();
	
	public static void init() {
		try {
			kbMap = (IntHashMap) RegUtils.getObfField(KeyBinding.class, "hash", "idk").get(null);
			pressedField = RegUtils.getObfField(KeyBinding.class, "pressed", "idk");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void override(int keyID) {
		if(activeOverrides.containsKey(keyID))
			return;
		KeyBinding kb = (KeyBinding) kbMap.removeObject(keyID);
		if(kb != null) {
			try {
				pressedField.set(kb, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			kb.setKeyCode(-1);
			activeOverrides.put(keyID, kb);
		}
	}
	
	public static void removeOverride(int keyID) {
		KeyBinding kb = activeOverrides.remove(keyID);
		if(kb != null) {
			kb.setKeyCode(keyID);
			kbMap.addKey(keyID, kb);
		}
	}
	
	private static void releaseLocks() {
		for(Map.Entry<Integer, KeyBinding> ao: activeOverrides.entrySet()) {
			kbMap.addKey(ao.getKey(), ao.getValue());
		}
	}
	
	private static void restoreLocks() {
		for(Map.Entry<Integer, KeyBinding> ao: activeOverrides.entrySet()) {
			try {
				pressedField.set(ao.getValue(), false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			kbMap.removeObject(ao.getKey());
		}
	}
	
	GuiScreen lastTickGui;
	@SubscribeEvent
	public void onClientTick(ClientTickEvent cte) {
		GuiScreen cgs = Minecraft.getMinecraft().currentScreen;
		if(lastTickGui == null && cgs != null) {
			releaseLocks();
		}
		if(lastTickGui != null && cgs == null) {
			restoreLocks();
		}
		lastTickGui = cgs;
	}
}
