package cn.academy.worldgen;

import cn.academy.AcademyCraft;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * This is the main registry of all the crafting materials. Oredict name and
 * Recipe script names are all provided here.
 * 
 * @author WeAthFolD, Shielian, KS
 */
public class WorldGenInit {
    
    // CONFIGS
    public static boolean GENERATE_ORES, GENERATE_PHASE_LIQUID;

    //@RegWorldGen(2)
    @StateEventCallback
    public static void preInit(FMLPreInitializationEvent event)
    {
        GameRegistry.registerWorldGenerator(worldGen,2);
    }
    public static final ACWorldGen worldGen = new ACWorldGen();

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        GENERATE_ORES = AcademyCraft.config.getBoolean("genOres", "generic", true, "Whether the ores will be generated in overworld.");
        GENERATE_PHASE_LIQUID = AcademyCraft.config.getBoolean("genPhaseLiquid", "generic", true, "Whether phase liquid will be generated in overworld.");
    }

}