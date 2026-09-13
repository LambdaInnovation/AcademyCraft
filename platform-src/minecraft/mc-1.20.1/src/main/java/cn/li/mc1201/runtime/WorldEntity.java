package cn.li.mc1201.runtime;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class WorldEntity {
    private WorldEntity() {
    }

    public static boolean isLivingEntity(Entity entity) {
        return entity instanceof LivingEntity;
    }

    public static List<LivingEntity> getLivingEntitiesInAabb(ServerLevel level, AABB aabb) {
        return level.getEntitiesOfClass(LivingEntity.class, aabb);
    }

    public static List<Entity> getEntitiesInAabb(ServerLevel level, AABB aabb) {
        return level.getEntitiesOfClass(Entity.class, aabb);
    }

    public static boolean spawnLightning(ServerLevel level, double x, double y, double z) {
        return spawnLightning(level, x, y, z, false);
    }

    /**
     * visualOnly matches original ThunderClap's EntityLightningBolt(world, x,
     * y, z, effectOnly=true): flash + thunder sound only, no vanilla damage,
     * no fire, no creeper-charging — the skill applies its own damage
     * separately, and a real bolt's side effects were never part of the
     * original design.
     */
    public static boolean spawnLightning(ServerLevel level, double x, double y, double z, boolean visualOnly) {
        LightningBolt lightning = (LightningBolt) EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) {
            return false;
        }
        lightning.moveTo(x, y, z);
        lightning.setVisualOnly(visualOnly);
        return level.addFreshEntity(lightning);
    }

    public static void createExplosion(
            ServerLevel level,
            Entity source,
            double x,
            double y,
            double z,
            float radius,
            boolean fire,
            boolean terrain) {
        Level.ExplosionInteraction interaction = terrain
            ? Level.ExplosionInteraction.MOB
            : Level.ExplosionInteraction.NONE;
        level.explode(source, x, y, z, radius, fire, interaction);
    }

    /**
     * Matches original EMDamageHelper.attack's creeper-charging side effect:
     * a skill hit on a creeper has some chance to power it, without the
     * incidental damage/fire/100%-radius side effects of a real lightning
     * bolt entity. Chance is rolled by the caller.
     *
     * Creeper has no public setPowered — the sanctioned API is
     * Creeper#thunderHit(ServerLevel, LightningBolt), which vanilla itself
     * uses when a real bolt strikes one. The LightningBolt passed in is
     * marked visualOnly and never added to the level, so it never ticks,
     * deals its own incidental damage, or ignites anything — it exists only
     * as the parameter thunderHit expects.
     */
    public static boolean tryPowerCreeper(ServerLevel level, Entity entity) {
        if (!(entity instanceof Creeper creeper)) {
            return false;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return false;
        }
        bolt.setVisualOnly(true);
        bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
        creeper.thunderHit(level, bolt);
        return true;
    }
}
