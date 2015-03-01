/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.ctrl;

import net.minecraftforge.common.config.ConfigCategory;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ctrl.EventHandlerClient;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;

/**
 * Used by EventHandlerClient to store, find, switch, and modify presets.
 * @author acaly
 *
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
public class PresetManager {
	
	/**
	 * The number of presets a player can have.
	 */
	public static final int PRESET_COUNT = 4;
	
	/*
	 * Internally used constants for configuration. 
	 */
	private static final String[] PRESET_CONFIG_NAME = new String[] {
		"preset_1", "preset_2", "preset_3", "preset_4"
	};
	private static final String PRESET_CONFIG_DEFAULT = "preset_default";
	private static final String PRESET_CONFIG_GLOBAL_WORLD = "preset_world_";
	private static final String PRESET_CONFIG_GLOBAL_OTHER = "preset_global";
	private static final String PRESET_CONFIG_ID = "world_id";
	
	
	/**
	 * The next available world id. Updated and saved to config file when exiting a server.
	 */
	private static int nextWorldId;
	
	/**
	 * The world id of current PresetManager.
	 */
	private int worldId;
	
	/**
	 * Preset data
	 */
	private Preset[] presets;
	
	/**
	 * Index of current preset;
	 */
	private int currentId;
	
	/**
	 * Get the nextWorldId saved in config file.
	 * Called by Proxy when loading.
	 */
	public static void init() {
		nextWorldId = AcademyCraft.config.get(PRESET_CONFIG_GLOBAL_OTHER, PRESET_CONFIG_ID, 0).getInt();
	}
	
	public void reset() {
		for (int i = 0; i < PRESET_COUNT; ++i) {
			presets[i] = new Preset();
		}
		save();
	}
	
	/**
	 * Save presets to config file.
	 * Called by EventHandlerClient when exiting a world.
	 */
	public void save() {
		//First check the nextWorldId
		if (worldId == nextWorldId) {
			++nextWorldId;
			AcademyCraft.config.get(PRESET_CONFIG_GLOBAL_OTHER, PRESET_CONFIG_ID, 0).set(nextWorldId);
		}

		String worldIdStr = PRESET_CONFIG_GLOBAL_WORLD + worldId;
		ConfigCategory cat = AcademyCraft.config.getCategory(worldIdStr);
		//Remove existing data
		cat.clear();
		//Add new data
		for (int i = 0; i < PRESET_COUNT; ++i) {
			//Use get to save integer array
			AcademyCraft.config.get(worldIdStr, PRESET_CONFIG_NAME[i], presets[i].data);
		}
		
		AcademyCraft.config.save();
	}
	
	/**
	 * Create a new PresetManager with the given worldId.
	 * @param worldId The world id.
	 */
	public PresetManager(int worldId) {
		this.worldId = worldId;
		
		String worldIdStr = PRESET_CONFIG_GLOBAL_WORLD + worldId;
		
		presets = new Preset[PRESET_COUNT];
		for (int i = 0; i < PRESET_COUNT; ++i) {
			presets[i] = new Preset(AcademyCraft.config.get(
					worldIdStr, PRESET_CONFIG_NAME[i], new int[] {0, 0, 0, 0}));
		}
		
		int def = AcademyCraft.config.get(worldIdStr, PRESET_CONFIG_DEFAULT, 0).getInt();
		if (def < 0 || def >= PRESET_COUNT) {
			def = 0;
		}
		currentId = def;
	}
	
	/**
	 * Get nextWorldId.
	 * @return Result.
	 */
	public static int getNextWorldId() {
		return nextWorldId;
	}
	
	/**
	 * Get the worldId used to initialize this instance.
	 * @return Result.
	 */
	int getWorldId() {
		return worldId;
	}
	
	/**
	 * Use this function to get the skill mapping.
	 * @param keyId Key id (0-3)
	 * @return Skill id.
	 */
	public static int getSkillMapping(int keyId) {
		PresetManager pm = getInstance();
		if (pm == null) {
			AcademyCraft.log.error("Try to get Skill Mapping when PresetManager is not initialized.");
			return 0;
		}
		return pm.presets[pm.currentId].getSkillMapping(keyId);
	}
	
	/**
	 * Get the preset with given id.
	 * @param presetId
	 * @return
	 */
	public static Preset getPreset(int presetId) {
		return getInstance().presets[presetId];
	}
	
	public static void setPreset(int pid, Preset pr) {
		if(pr == null) return;
		PresetManager presets = getInstance();
		presets.presets[pid] = pr;
	}
	
	/**
	 * Get the id of current preset.
	 * @return
	 */
	public static int getCurrentPresetId() {
		return getInstance().currentId;
	}
	
	/**
	 * Set the id of current preset.
	 * @param presetId
	 */
	public static void setCurrentPreset(int presetId) {
		getInstance().currentId = presetId;
	}
	
	public static Preset getCurrentPreset() {
		PresetManager presets = getInstance();
		return presets.presets[presets.currentId];
	}
	
	private static PresetManager getInstance() {
		PresetManager presets = EventHandlerClient.getPresetManager();
		if (presets == null) {
			AcademyCraft.log.fatal("Cannot get the PresetManager instance.");
			throw new RuntimeException("Cannot get the PresetManager instance.");
		}
		return presets;
	}
}
