package cn.academy.ability.api;

import cn.academy.core.AcademyCraft;
import cn.academy.core.config.ACConfig;
import cn.academy.core.event.BlockDestroyEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.function.Predicate;

/**
 * Global uniform utils handling common actions of skills.
 *
 * For per-skill actions, consider using {@link AbilityContext} instead
 */
@Registrant
public class AbilityPipeline {

    private AbilityPipeline() {}

    /**
     * @return Whether we can break any block at all
     */
    public static boolean canBreakBlock() {
        return propDestroyBlocks.getBoolean();
    }

    /**
     * @return Whether PvP is enabled.
     */
    public static boolean canAttackPlayer() {
        return propAttackPlayer.getBoolean();
    }

    /**
     * Tests if we break the block at the specified coordinates.
     * @return Whether the block can be really broken
     */
    public static boolean canBreakBlock(World world, int x, int y, int z) {
        return !MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, x, y, z));
    }

    /**
     * Apply a range attack on a specific point and range. The damage attenuates linearly.
     * At the center, the damage is [damage], at the edge the damage is 0.
     * @deprecated use {@link AbilityContext} instead
     */
    public static void applyRangeAttack(
            EntityPlayer player,
            double x, double y, double z, double range,
            float damage, Skill skill,
            Predicate<Entity> entitySelector) {
        AbilityContext.of(player, skill).attackRange(x, y, z, range, damage, entitySelector);
    }

    private static float getDamageScale() {
        return (float) ACConfig.instance().getDouble(
                "ac.ability.calc_global.damage_scale"
        );
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
