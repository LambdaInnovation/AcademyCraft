package cn.academy.ability;

import cn.academy.AcademyCraft;
import cn.academy.event.BlockDestroyEvent;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.lang3.ArrayUtils;

/**
 * INTERNAL CLASS
 */
public class AbilityPipeline {

    private AbilityPipeline() {}

    /**
     * @return Whether we can break any block at all
     */
    static boolean canBreakBlock(World world) {
        return propDestroyBlocks.getBoolean() ||
                ArrayUtils.contains(propWorldsDestroyingBlocks.getStringList(), String.valueOf(world.provider.getDimension())) ||
                ArrayUtils.contains(propWorldsDestroyingBlocks.getStringList(), world.provider.getSaveFolder());
                //||ArrayUtils.contains(propWorldsDestroyingBlocks.getStringList(), world.provider.g);
    }

    static boolean isAllWorldDisableBreakBlock() {
        return !propDestroyBlocks.getBoolean() && propWorldsDestroyingBlocks.getIntList().length == 0;
    }

    /**
     * @return Whether PvP is enabled.
     */
    static boolean canAttackPlayer() {
        return propAttackPlayer.getBoolean();
    }

    /**
     * Tests if the block at the specified coordinates can be broken by a
     * certain player.
     * @return Whether the block can be really broken.
     */
    static boolean canBreakBlock(World world, EntityPlayer player,
        int x, int y, int z)
    {
        return !MinecraftForge.EVENT_BUS.post(
            new BlockDestroyEvent(world, player, new BlockPos(x, y, z)));
    }
    static boolean canBreakBlock(World world, EntityPlayer player, BlockPos pos)
    {
        return !MinecraftForge.EVENT_BUS.post(
                new BlockDestroyEvent(world, player, pos));
    }

    public static boolean canUseMouseWheel(){
        return propUseMouseWheel.getBoolean();
    }

    /**
     * Tests if the block at the specified coordinates can be broken.
     * @return Whether the block can be really broken.
     */
    static boolean canBreakBlock(World world, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(
            new BlockDestroyEvent(world, new BlockPos(x, y, z)));
    }
    static boolean canBreakBlock(World world, BlockPos pos) {
        return !MinecraftForge.EVENT_BUS.post(
                new BlockDestroyEvent(world, pos));
    }

    /**
     * Tests if the block at the specified coordinates can be broken by a
     * certain player.
     * @return Whether the block can be really broken.
     */
    static boolean canBreakBlock(EntityPlayer player, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(
            new BlockDestroyEvent(player, new BlockPos(x, y, z)));
    }
    static boolean canBreakBlock(EntityPlayer player, BlockPos pos) {
        return !MinecraftForge.EVENT_BUS.post(
                new BlockDestroyEvent(player, pos));
    }

    // PROPERTIES
    private static Property propAttackPlayer;
    private static Property propDestroyBlocks;
    private static Property propWorldsDestroyingBlocks;
    private static Property propUseMouseWheel;

    @StateEventCallback
    private static void _init(FMLInitializationEvent event) {
        Configuration conf = AcademyCraft.config;

        propAttackPlayer = conf.get("generic", "attackPlayer", true, "Whether the skills are effective on players.");
        propDestroyBlocks = conf.get("generic", "destroyBlocks", true, "Whether the skills will destroy blocks in the world.");
        propWorldsDestroyingBlocks = conf.get("generic", "worldsWhitelistedDestroyingBlocks", new String[]{},
                "The worlds which whitelisted destroying blocks. World IDs, sub folder names and world names are all supported.");
        propUseMouseWheel = conf.get("generic","useMouseWheel",false,"Whether teleporter can use mouse wheel to control the destination.");
        MinecraftForge.EVENT_BUS.register(new AbilityPipeline());
    }

    @SubscribeEvent
    public void onBlockDestroy(BlockDestroyEvent event) {
        if(!canBreakBlock(event.world))
            event.setCanceled(true);
    }

}