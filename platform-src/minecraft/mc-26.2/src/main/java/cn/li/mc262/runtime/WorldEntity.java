package cn.li.mc262.runtime;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Server-side world/entity helpers for 26.2.
 * EntityType.create now requires {@link EntitySpawnReason}.
 */
public final class WorldEntity {
    private static final Field LARGE_FIREBALL_EXPLOSION_POWER;

    static {
        Field field;
        try {
            field = LargeFireball.class.getDeclaredField("explosionPower");
            field.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            field = null;
        }
        LARGE_FIREBALL_EXPLOSION_POWER = field;
    }

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

    public static boolean spawnLightning(ServerLevel level, double x, double y, double z, boolean visualOnly) {
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning == null) {
            return false;
        }
        lightning.snapTo(x, y, z);
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

    public static boolean tryPowerCreeper(ServerLevel level, Entity entity) {
        if (!(entity instanceof Creeper creeper)) {
            return false;
        }
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return false;
        }
        bolt.setVisualOnly(true);
        bolt.snapTo(entity.getX(), entity.getY(), entity.getZ());
        creeper.thunderHit(level, bolt);
        return true;
    }

    /**
     * LargeFireball.explosionPower is private with no public setter on 26.2;
     * CompoundTag save/load was replaced by ValueIO. Reflect for the call surface.
     */
    public static Integer getLargeFireballExplosionPower(Entity entity) {
        if (!(entity instanceof LargeFireball) || LARGE_FIREBALL_EXPLOSION_POWER == null) {
            return null;
        }
        try {
            return LARGE_FIREBALL_EXPLOSION_POWER.getInt(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static boolean setLargeFireballExplosionPower(Entity entity, int power) {
        if (!(entity instanceof LargeFireball) || LARGE_FIREBALL_EXPLOSION_POWER == null) {
            return false;
        }
        try {
            LARGE_FIREBALL_EXPLOSION_POWER.setInt(entity, power);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
