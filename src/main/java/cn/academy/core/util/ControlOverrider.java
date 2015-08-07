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
package cn.academy.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.IntHashMap;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.RegInit;
import cn.liutils.core.LIUtils;
import cn.liutils.util.generic.RegistryUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This class overrides(i.e.Disables) vanilla minecraft's control on a certain key. 
 * It is currently intented to be used DURING gameplay and will unlock all overrides when any GUI is present.
 * There are two ways to use ControlOverrider: activate/deactivate a key manually, or add key event filters into it.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegInit
@RegEventHandler(Bus.Forge)
public class ControlOverrider {
	
	private static IntHashMap kbMap;
	private static Field pressedField;
	private static Field kbMapField;
	
	private static Map<Integer, Override> activeOverrides = new HashMap();
	
	private static boolean completeOverriding;
	
	public static void init() {
		try {
			kbMapField = RegistryUtils.getObfField(KeyBinding.class, "hash", "field_74514_b");
			kbMap = getOriginalKbMap();
			
			pressedField = RegistryUtils.getObfField(KeyBinding.class, "pressed", "field_74513_e");
			Field modifiersField = Field.class.getDeclaredField("modifiers");
		    modifiersField.setAccessible(true);
		    modifiersField.setInt(kbMapField, kbMapField.getModifiers() & (~Modifier.FINAL));
		} catch(Exception e) {
			error("init", e);
			e.printStackTrace();
		}
	}
	
	private static IntHashMap createCopy(IntHashMap from, IntHashMap to) {
		if(to == null)
			to = new IntHashMap();
		// Awkward, but who knows if this is faster than reflection?
		for(int i = -100; i <= 250; ++i) {
			if(from.containsItem(i))
				to.addKey(i, from.lookup(i));
		}
		return to;
	}
	
	private static IntHashMap getOriginalKbMap() {
		try {
			return (IntHashMap) kbMapField.get(null);
		} catch (Exception e) {
			error("getOriginalKbMap", e);
			e.printStackTrace();
			return null;
		}
	}
	
	// SUPERHACKTECH Starts
	/**
	 * A complete override stops ALL minecraft keys to function. Currently you can open up complete override globally only once.
	 */
	public static void startCompleteOverride() {
		if(!completeOverriding) {
			completeOverriding = true;
			kbMap = createCopy(kbMap, null);
			getOriginalKbMap().clearMap();
		}
	}
	
	public static void endCompleteOverride() {
		if(completeOverriding) {
			completeOverriding = false;
			createCopy(kbMap, getOriginalKbMap());
			kbMap = getOriginalKbMap();
		} else {
			error("Try to stop complete override while not overriding at all", new RuntimeException());
		}
	}
	// SUPERHACKTECH Ends
	
	public static void override(int keyID) {
		if(activeOverrides.containsKey(keyID)) {
			activeOverrides.get(keyID).count++;
			if(activeOverrides.get(keyID).count > 100)
				AcademyCraft.log.warn("Over 100 override locks for " + 
						keyID + ". Might be a programming error?");
			log("Override increment " + "[" + keyID + "]" + activeOverrides.get(keyID).count);
			return;
		}
		
		KeyBinding kb = (KeyBinding) kbMap.removeObject(keyID);
		if(kb != null) {
			try {
				pressedField.set(kb, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//kb.setKeyCode(-1);
			activeOverrides.put(keyID, new Override(kb));
			log("Override new [" + keyID + "]");
		} else {
			log("Override ignored [" + keyID + "]");
		}
	}
	
	public static void removeOverride(int keyID) {
		Override ovr = activeOverrides.get(keyID);
		if(ovr == null)
			return;
		
		if(ovr.count > 1) {
			ovr.count--;
			
			log("Override decrement [" + keyID + "]" + ovr.count);
		} else {
			activeOverrides.remove(keyID);
			
			ovr.kb.setKeyCode(keyID);
			kbMap.addKey(keyID, ovr.kb);
			
			log("Override remove [" + keyID + "]");
		}
	}
	
	private static void releaseLocks() {
		for(Map.Entry<Integer, Override> ao: activeOverrides.entrySet()) {
			kbMap.addKey(ao.getKey(), ao.getValue().kb);
		}
	}
	
	private static void restoreLocks() {
		for(Map.Entry<Integer, Override> ao: activeOverrides.entrySet()) {
			try {
				pressedField.set(ao.getValue().kb, false);
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
	
	private static void log(String s) {
		if(AcademyCraft.DEBUG_MODE)
			AcademyCraft.log.info(s);
	}
	
	private static void error(String s, Exception e) {
		AcademyCraft.log.error("ControlOverrider error: " + s, e);
	}
	
	private static class Override {
		final KeyBinding kb;
		int count;
		
		public Override(KeyBinding _kb) {
			kb = _kb;
			count = 1;
		}
	}
}
