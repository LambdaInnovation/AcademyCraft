package cn.academy.ability.api.ctrl;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.preset.PresetData;
import cn.academy.core.ModuleCoreClient;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.KeyManager;

/**
 * Key event listener for skill events.
 * @author acaly
 *
 */
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
    	
        //TODO: Implement
//        LIKeyProcess.instance.addKey("Skill 1", Keyboard.KEY_R, false, new IKeyHandler() {
//            
//            SkillInstance down;
//            
//            @Override
//            public void onKeyDown(int keyCode, boolean tickEnd) {
//                if(tickEnd || !ClientUtils.isPlayerInGame()) return;
//                //TODO check preset ready and skill enabled
//                down = Skill.testSkill.createSkillInstance(Minecraft.getMinecraft().thePlayer);
//            }
//
//            @Override
//            public void onKeyUp(int keyCode, boolean tickEnd) {
//                if (down != null) {
//                    down.onClientKeyUp();
//                    down = null;
//                }
//            }
//
//            @Override
//            public void onKeyTick(int keyCode, boolean tickEnd) {
//            }
//            
//        });
        
    }
    
    private static class AbilityKey extends KeyHandler {
    	
    	final int internalID;
    	
    	public AbilityKey(int id) {
    		internalID = id;
    	}
    	
    	//TODO Implement
    	
    }
    
}
