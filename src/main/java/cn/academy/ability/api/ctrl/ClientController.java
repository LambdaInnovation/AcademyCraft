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
package cn.academy.ability.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.ctrl.SkillInstance.State;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.ability.api.event.AbilityActivateEvent;
import cn.academy.ability.api.event.AbilityDeactivateEvent;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.ability.api.event.PresetUpdateEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ModuleCoreClient;
import cn.academy.core.util.ControlOverrider;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegInit;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.KeyManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This class handles the ability key and their controlling, 
 * and the overriding of vanilla MC control.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegInit
@RegEventHandler
public class ClientController {
	
	public static final int MAX_KEYS = PresetData.MAX_KEYS, STATIC_KEYS = 4;
	
	private static AbilityKey[] handlers = new AbilityKey[MAX_KEYS];
	
	public static final int[] defaultMapping = new int[] { 
		KeyManager.MOUSE_LEFT, 
		KeyManager.MOUSE_RIGHT, 
		Keyboard.KEY_R, 
		Keyboard.KEY_F 
	};
	
	public static int getKeyMapping(int kid) {
		if(kid < STATIC_KEYS) {
			return ModuleCoreClient.keyManager.getKeyID(handlers[kid]);
		} else {
			return ModuleCoreClient.dynKeyManager.getKeyID(handlers[kid]);
		}
	}
	
	/**
	 * Remaps a Special Key.
	 */
	public static void remap(int id, int keyID) {
		if(id >= 4 || id < 0)
			throw new IllegalStateException("id overflow");
		ModuleCoreClient.dynKeyManager.resetBindingKey("ability_" + (id + 4), keyID);
	}
    
    public static void init() {
    	for(int i = 0; i < STATIC_KEYS; ++i) {
    		ModuleCoreClient.keyManager.addKeyHandler("ability_" + i, defaultMapping[i], handlers[i] = new AbilityKey(i));
    	}
    	
    	for(int i = STATIC_KEYS; i < MAX_KEYS; ++i) {
    		ModuleCoreClient.dynKeyManager.addKeyHandler("ability_" + i, 0, handlers[i] = new AbilityKey(i));
    	}
    }
    
    private static boolean hasMutexInstance() {
    	return getMutexInstance() != null;
    }
    
    public static SkillInstance getMutexInstance() {
    	for(AbilityKey key : handlers) {
    		if(key.instance != null && key.instance.isMutex)
    			return key.instance;
    	}
    	return null;
    }
    
    static AbilityKey getMutexHandler() {
    	for(AbilityKey key : handlers) {
    		if(key.instance != null && key.instance.isMutex)
    			return key;
    	}
    	return null;
    }
    
    /**
     * Stores KEYID in case the key mapping is editted.
     */
    private Integer[] lastOverrides;
    private boolean overrideInit;
    
    @SubscribeEvent
    public void changePreset(PresetSwitchEvent event) {
    	if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
    		rebuildOverrides();
    }
    
    @SubscribeEvent
    public void editPreset(PresetUpdateEvent event) {
    	if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
    		rebuildOverrides();
    }
    
    @SubscribeEvent
    public void activate(AbilityActivateEvent event) {
    	if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
    		rebuildOverrides();
    }
    
    @SubscribeEvent
    public void deactivate(AbilityDeactivateEvent event) {
    	if(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
    		rebuildOverrides();
    }
    
    private void rebuildOverrides() {
    	EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    	//if(player == null)
    	//	return;
    	
    	PresetData pdata = PresetData.get(player);
    	CPData cpData = CPData.get(player);
    	
    	if(lastOverrides != null) {
    		for(int i : lastOverrides)
    			ControlOverrider.removeOverride(i);
    	}
    	
    	if(cpData.isActivated()) {
	    	Preset preset = pdata.getCurrentPreset();
	    	if(preset != null) {
	    		List<Integer> list = new ArrayList();
	    		
		    	for(int i = 0; i < MAX_KEYS; ++i) {
		    		if(preset.hasMapping(i)) {
		    			Controllable c = preset.getControllable(i);
		    			if(c.shouldOverrideKey()) {
			    			int mapping = getKeyMapping(i);
			    			
			    			list.add(mapping);
			    			ControlOverrider.override(mapping);
		    			}
		    		}
		    	}
		    	
		    	lastOverrides = list.toArray(new Integer[] {});
    	}
    	}
    }
    
    static class AbilityKey extends KeyHandler {
    	
    	final int internalID;
    	
    	SkillInstance instance;
    	
    	public AbilityKey(int id) {
    		internalID = id;
    	}
    	
    	@Override
    	public void onKeyDown() {
    		if(instance != null) {
    			instance.onAbort();
    			instance = null;
    		}
    		
    		CPData cpData = CPData.get(getPlayer());
    		if(cpData.isActivated() && cpData.canUseAbility()) {
	    		instance = locate();
	    		if(instance != null) {
	    			instance.ctrlStarted();
	    		}
    		}
    	}
    	
    	@Override
    	public void onKeyTick() {
    		if(instance != null) {
    			if(instance.state == State.ENDED) {
    				instance.ctrlEnded();
    				instance = null;
    			} else if(instance.state == State.ABORTED) {
    				instance.ctrlAborted();
    				instance = null;
    			} else {
    				instance.ctrlTick();
    			}
    		}
    	}
    	
    	@Override
    	public void onKeyUp() {
    		if(instance != null) {
    			instance.ctrlEnded();
    			instance = null;
    		}
    	}
    	
    	@Override
    	public void onKeyAbort() {
    		if(instance != null) {
    			instance.ctrlAborted();
    			instance = null;
    		}
    	}
    	
    	private SkillInstance locate() {
    		PresetData pdata = PresetData.get(getPlayer());
    		if(!pdata.isActive())
    			return null;
    		
    		Controllable cc = pdata.getCurrentPreset().getControllable(internalID);
    		if(Cooldown.isInCooldown(cc) || cc == null) return null;
    		
    		SkillInstance instance = cc.createSkillInstance(getPlayer());
    		if(instance != null && instance.isMutex && hasMutexInstance())
    			instance = null;
    		
    		if(instance == null) {
    			AcademyCraft.log.warn("NULL SkillInstance for " + cc);
    		} else
    			instance.controllable = cc;
    		
    		return instance;
    	}
    	
    }
}
