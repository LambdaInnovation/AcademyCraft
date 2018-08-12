package cn.academy.worldgen;

import cn.academy.AcademyCraft;

/**
 * This is the main registry of all the crafting materials. Oredict name and
 * Recipe script names are all provided here.
 * 
 * @author WeAthFolD, Shielian, KS
 */
@RegACRecipeNames
public class WorldGenInit {
    
    // CONFIGS
    public static boolean GENERATE_ORES, GENERATE_PHASE_LIQUID;

    @RegWorldGen(2)
    public static ACWorldGen worldGen;

    @RegInitCallback
    private static void init() {
        GENERATE_ORES = AcademyCraft.config.getBoolean("genOres", "generic", true, "Whether the ores will be generated in overworld.");
        GENERATE_PHASE_LIQUID = AcademyCraft.config.getBoolean("genPhaseLiquid", "generic", true, "Whether phase liquid will be generated in overworld.");
    }

}