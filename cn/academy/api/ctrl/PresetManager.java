package cn.academy.api.ctrl;

import cn.academy.api.ability.Category;

/**
 * Used by EventHandlerClient to store, find, switch, and modify presets.
 * @author acaly
 *
 */
public class PresetManager {
	
	public PresetManager(Category cat, int worldId) {
		
	}
	
	public int getWorldId() {
		return 0;
	}
	
	public int getSkillMapping(int keyId) {
		return keyId; //TODO
	}
	
	public static int getNextWorldId() {
		return 0;
	}
}
