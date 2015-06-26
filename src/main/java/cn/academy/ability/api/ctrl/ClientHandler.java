package cn.academy.ability.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.CPData;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.PresetData;
import cn.academy.ability.api.ctrl.SkillInstance.State;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ModuleCoreClient;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.KeyManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Key event listener for skill events. <br>
 * TODO: We might wanna setup a Activate Key callback to do something else in certain envs.
 * 	e.g. When in special ability mode and want to cancel the skill,
 * 		or when using charge skills and want to cancel the charging...
 * @author acaly, WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegInit
public final class ClientHandler {
	
	public static final int MAX_KEYS = PresetData.MAX_KEYS, STATIC_KEYS = 4;
	
	private static AbilityKey[] handlers = new AbilityKey[MAX_KEYS];
	
	private static final int[] defaultMapping = new int[] { 
		KeyManager.MOUSE_LEFT, 
		KeyManager.MOUSE_RIGHT, 
		Keyboard.KEY_R, 
		Keyboard.KEY_F 
	};
	
	static final int ACTIVATE_KEY_MAPPING = Keyboard.KEY_V;
	
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
    	
    	ModuleCoreClient.keyManager.addKeyHandler("ability_activate", ACTIVATE_KEY_MAPPING, new ActivateKey());
        
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
    		
    		if(CPData.get(getPlayer()).isActivated()) {
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
    		if(Cooldown.isInCooldown(cc) || cc == null) return null;
    		
    		SkillInstance instance = cc.createSkillInstance(getPlayer());
    		if(instance == null) {
    			AcademyCraft.log.warn("NULL SkillInstance for " + cc);
    		} else
    			instance.controllable = cc;
    		
    		return instance;
    	}
    	
    }
    
    /**
     * The key to activate and deactivate the ability, might have other use in certain circumstances,
     *  e.g. quit charging when using ability.
     */
    private static class ActivateKey extends KeyHandler {
    	
    	@Override
    	public void onKeyDown() {
    		EntityPlayer player = getPlayer();
    		AbilityData aData = AbilityData.get(player);
    		CPData cpData = CPData.get(player);
    		
    		if(aData.isLearned()) {
    			if(cpData.isActivated()) {
    				System.out.println("Deactivated.");
    				ControlSyncs.deactivateAtServer(cpData);
    			} else {
    				System.out.println("Activated.");
    				ControlSyncs.activateAtServer(cpData);
    			}
    		}
    	}
    	
    }
    
}
