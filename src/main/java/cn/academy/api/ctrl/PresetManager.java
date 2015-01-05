package cn.academy.api.ctrl;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import cn.academy.api.ability.Category;
import cn.academy.core.AcademyCraftMod;

/**
 * Used by EventHandlerClient to store, find, switch, and modify presets.
 * @author acaly
 *
 */
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
		nextWorldId = AcademyCraftMod.config.get(PRESET_CONFIG_GLOBAL_OTHER, PRESET_CONFIG_ID, 0).getInt();
	}
	
	void reset() {
		for (int i = 0; i < PRESET_COUNT; ++i) {
			presets[i] = new Preset();
		}
		save();
	}
	
	/**
	 * Save presets to config file.
	 * Called by EventHandlerClient when exiting a world.
	 */
	void save() {
		//First check the nextWorldId
		if (worldId <= nextWorldId) {
			++nextWorldId;
			AcademyCraftMod.config.get(PRESET_CONFIG_GLOBAL_OTHER, PRESET_CONFIG_ID, 0).set(nextWorldId);
		}

		String worldIdStr = PRESET_CONFIG_GLOBAL_WORLD + worldId;
		ConfigCategory cat = AcademyCraftMod.config.getCategory(worldIdStr);
		//Remove existing data
		cat.clear();
		//Add new data
		for (int i = 0; i < PRESET_COUNT; ++i) {
			//Use get to save integer array
			AcademyCraftMod.config.get(worldIdStr, PRESET_CONFIG_NAME[i], presets[i].data);
		}
		
		AcademyCraftMod.config.save();
	}
	
	/**
	 * Create a new PresetManager with the given worldId.
	 * @param worldId The world id.
	 */
	PresetManager(int worldId) {
		this.worldId = worldId;
		
		String worldIdStr = PRESET_CONFIG_GLOBAL_WORLD + worldId;
		
		presets = new Preset[PRESET_COUNT];
		for (int i = 0; i < PRESET_COUNT; ++i) {
			presets[i] = new Preset(AcademyCraftMod.config.get(
					worldIdStr, PRESET_CONFIG_NAME[i], new int[] {0, 0, 0, 0}));
		}
		
		int def = AcademyCraftMod.config.get(worldIdStr, PRESET_CONFIG_DEFAULT, 0).getInt();
		if (def < 0 || def >= PRESET_COUNT) {
			def = 0;
		}
		currentId = def;
	}
	
	/**
	 * Get nextWorldId.
	 * @return Result.
	 */
	static int getNextWorldId() {
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
			AcademyCraftMod.log.error("Try to get Skill Mapping when PresetManager is not initialized.");
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
			AcademyCraftMod.log.fatal("Cannot get the PresetManager instance.");
			throw new RuntimeException("Cannot get the PresetManager instance.");
		}
		return presets;
	}
}
