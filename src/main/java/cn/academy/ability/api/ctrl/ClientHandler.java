package cn.academy.ability.api.ctrl;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.PresetData;
import cn.academy.ability.api.ctrl.SkillInstance.State;
import cn.academy.core.ModuleCoreClient;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.KeyManager;
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
	
	private static int[] defaultMapping = new int[] { KeyManager.MOUSE_LEFT, KeyManager.MOUSE_RIGHT, Keyboard.KEY_R, Keyboard.KEY_F };
	
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
    		return cc == null ? null : cc.createSkillInstance(getPlayer());
    	}
    	
    }
    
}
