package cn.academy.ability.api;

import cn.academy.core.AcademyCraft;
import cn.academy.core.event.BlockDestroyEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * INTERNAL CLASS
 */
@Registrant
public class AbilityPipeline {

    private AbilityPipeline() {}

    /**
     * @return Whether we can break any block at all
     */
    static boolean canBreakBlock() {
        return propDestroyBlocks.getBoolean();
    }

    /**
     * @return Whether PvP is enabled.
     */
    static boolean canAttackPlayer() {
        return propAttackPlayer.getBoolean();
    }

    /**
     * Tests if we break the block at the specified coordinates.
     * @return Whether the block can be really broken
     */
    static boolean canBreakBlock(World world, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, x, y, z));
    }

    // PROPERTIES
    private static Property propAttackPlayer;
    private static Property propDestroyBlocks;

    @RegInitCallback
    public static void _init() {
        Configuration conf = AcademyCraft.config;

        propAttackPlayer = conf.get("generic", "attackPlayer", true, "Whether the skills are effective on players.");
        propDestroyBlocks = conf.get("generic", "destroyBlocks", true, "Whether the skills will destroy blocks in the world.");

        MinecraftForge.EVENT_BUS.register(new AbilityPipeline());
    }

    @SubscribeEvent
    public void onBlockDestroy(BlockDestroyEvent event) {
        if(!canBreakBlock())
            event.setCanceled(true);
    }

}
