package cn.academy.ability.api.ctrl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.PresetData;
import cn.academy.ability.api.ctrl.SkillInstance.State;
import cn.academy.core.ModuleCoreClient;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.RegInit;
import cn.liutils.util.client.ClientUtils;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.KeyManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Key event listener for skill events.
 * @author acaly, WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegInit
public final class ClientHandler {
	
	public static final int MAX_KEYS = PresetData.MAX_KEYS, STATIC_KEYS = 4;
	
	private static AbilityKey[] handlers = new AbilityKey[MAX_KEYS];
	
	private static int[] defaultMapping = new int[] { 
		KeyManager.MOUSE_LEFT, 
		KeyManager.MOUSE_RIGHT, 
		Keyboard.KEY_R, 
		Keyboard.KEY_F 
	};
	
	private static Map<Controllable, Integer> cooldown = new HashMap();
	
	public static int getKeyMapping(int kid) {
		if(kid < STATIC_KEYS) {
			return ModuleCoreClient.keyManager.getKeyID(handlers[kid]);
		} else {
			return ModuleCoreClient.dynKeyManager.getKeyID(handlers[kid]);
		}
	}
    
    public static void init() {
    	
    	for(int i = 0; i < STATIC_KEYS; ++i) {
    		ModuleCoreClient.keyManager.addKeyHandler("ability_" + i, defaultMapping[i], handlers[i] = new AbilityKey(i));
    	}
    	
    	for(int i = STATIC_KEYS; i < MAX_KEYS; ++i) {
    		ModuleCoreClient.dynKeyManager.addKeyHandler("ability_" + i, 0, handlers[i] = new AbilityKey(i));
    	}
        
    }
    
    static void setCooldown(Controllable c, int cd) {
    	cooldown.put(c, cd);
    }
    
    public static boolean isInCooldown(Controllable c) {
    	return cooldown.containsKey(c);
    }
    
    private static void updateCooldown() {
    	Iterator< Entry<Controllable, Integer> > iter = cooldown.entrySet().iterator();
    	
    	while(iter.hasNext()) {
    		Entry< Controllable, Integer > entry = iter.next();
    		if(entry.getValue() == 0) {
    			iter.remove();
    		} else {
    			entry.setValue(entry.getValue() - 1);
    		}
    	}
    }
    
    private static class AbilityKey extends KeyHandler {
    	
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
    		
    		instance = locate();
    		if(instance != null) {
    			instance.onStart();
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
    				instance.onTick();
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
    		if(isInCooldown(cc) || cc == null) return null;
    		
    		SkillInstance instance = cc.createSkillInstance(getPlayer());
    		instance.controllable = cc;
    		
    		return instance;
    	}
    	
    }
    
    @RegEventHandler(Bus.FML)
    public static class Events {
    	
    	@SubscribeEvent
    	public void onClientTick(ClientTickEvent event) {
    		if(event.phase == Phase.END && ClientUtils.isPlayerInGame()) {
    			updateCooldown();
    		}
    	}
    	
    }
    
}
