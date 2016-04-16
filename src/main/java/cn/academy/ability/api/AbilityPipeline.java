package cn.academy.ability.api;

import cn.academy.ability.SkillDamageSource;
import cn.academy.ability.api.event.CalcEvent;
import cn.academy.ability.api.event.CalcEvent.SkillAttack;
import cn.academy.ability.api.event.ReflectEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.event.BlockDestroyEvent;
import cn.academy.core.event.ConfigModifyEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.List;
import java.util.function.Predicate;

/**
 * Uniform utils handling common actions of skills.
 */
@Registrant
public class AbilityPipeline {

    private AbilityPipeline() {}

    /**
     * Make the player fire the attack with the given skill. The damage will be re-calculated according to the damage
     *  scale specified globally and skill-locally, and influenced by passive skills.
     * @param target The entity to attack
     * @param player The player starts the attack
     * @param skill  The skill used
     * @param damage The amount of damage applied (raw)
     */
    public static void attack(EntityPlayer player, Skill skill, Entity target, float damage) {
        damage = CalcEvent.calc(new SkillAttack(player, skill, target, damage));

        if (damage > 0 && (canAttackPlayer() || (!(target instanceof EntityPlayer)))) {
            target.attackEntityFrom(new SkillDamageSource(player, skill), damage);
        }
    }

    /**
     * Performs a skill attack equivalent to {@link #attack}, but has the chance to be "reflected". (Currently used by
     *  solely Vector Manipulation). When reflected, the attack will NOT be applied and the reflectCallback will be
     *  invoked.
     * @param reflectCallback Will get called once reflection happens.
     */
    public static void attackReflect(EntityPlayer           player,
                                     Skill                  skill,
                                     Entity                 target,
                                     float                  damage,
                                     Runnable               reflectCallback) {
        if (MinecraftForge.EVENT_BUS.post(new ReflectEvent(player, skill, target))) {
            reflectCallback.run();
        } else {
            attack(player, skill, target, damage);
        }
    }

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
     */
    public static void applyRangeAttack(
            EntityPlayer player,
            double x, double y, double z, double range,
            float damage, Skill skill,
            Predicate<Entity> entitySelector) {
        List<Entity> list = WorldUtils.getEntities(player.worldObj, x, y, z, range, entitySelector);
        for(Entity ent : list) {
            double dist = MathUtils.distance(x, y, z, ent.posX, ent.posY, ent.posZ);
            float factor = 1 - MathUtils.clampf(0, 1, (float) (dist / range));
            float appliedDamage = MathUtils.lerpf(0, damage, factor);
            attack(player, skill, ent, appliedDamage);
        }
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
