package cn.li.mc1211.runtime;

import cn.li.mcver.ResourceLocations;

import cn.li.mcmod.ModId;
import cn.li.mc1211.entity.ScriptedEffectEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ParticleEntity {
    private ParticleEntity() {
    }

    public static ParticleOptions getParticleType(String particleType) {
        if (particleType != null && !particleType.isEmpty()) {
            try {
                ResourceLocation id = particleType.contains(":")
                    ? ResourceLocations.parse(particleType)
                    : ResourceLocations.of(ModId.ID, particleType.replace('-', '_'));
                ParticleType<?> dynamicType = BuiltInRegistries.PARTICLE_TYPE.get(id);
                if (dynamicType instanceof ParticleOptions options) {
                    return options;
                }
            } catch (Exception ignored) {
                // Fall back to predefined aliases below.
            }
        }
        return switch (particleType) {
            case "electric-spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "portal" -> ParticleTypes.PORTAL;
            case "flame" -> ParticleTypes.FLAME;
            case "smoke" -> ParticleTypes.SMOKE;
            case "end-rod" -> ParticleTypes.END_ROD;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "angry-villager" -> ParticleTypes.ANGRY_VILLAGER;
            case "totem-of-undying" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "generic" -> ParticleTypes.GLOW;
            default -> ParticleTypes.GLOW;
        };
    }

    public static boolean spawnEntityByIdFromPlayer(Object playerObj, String entityId, float speed) {
        return spawnEntityByIdFromPlayer(playerObj, entityId, speed, null);
    }

    public static boolean spawnEntityByIdFromPlayer(Object playerObj, String entityId, float speed, Integer lifeTicksOverride) {
        if (!(playerObj instanceof Player player) || entityId == null || entityId.isEmpty()) {
            return false;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return true;
        }
        // BuiltInRegistries.ENTITY_TYPE is a DefaultedRegistry — get() on an
        // unknown key silently returns the registry default (minecraft:pig),
        // which spawned a pig for a mistyped/missing entity id. Resolve
        // explicitly and throw with the offending id: a wrong id is a bug and
        // must surface in the log, never degrade into a random mob.
        ResourceLocation key;
        try {
            key = ResourceLocations.parse(entityId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed entity type id '" + entityId + "'", e);
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            throw new IllegalArgumentException(
                "Unknown entity type id '" + entityId + "' (resolves to " + key
                    + "; remember the modid namespace, e.g. ac:entity_coin_throwing)");
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
        if (type == null) {
            return false;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYRot(), player.getXRot());
        Vec3 look = player.getLookAngle().normalize().scale(speed);
        entity.setDeltaMovement(look);
        if (entity instanceof Projectile projectile) {
            projectile.setOwner(player);
        }
        if (entity instanceof ScriptedEffectEntity scriptedEffect) {
            scriptedEffect.setOwnerPlayer(player);
            scriptedEffect.setPos(player.getX(), player.getY() + 1.0D, player.getZ());
            if (lifeTicksOverride != null && lifeTicksOverride > 0) {
                scriptedEffect.setLifeTicksOverride(lifeTicksOverride);
            }
        }
        return level.addFreshEntity(entity);
    }

    /**
     * Same spawn as spawnEntityByIdFromPlayer, but returns the spawned
     * entity's UUID string (or null on failure) instead of a bare boolean —
     * lets callers reference the entity again later (e.g. MagManip's
     * held-block tracking via the entity-motion adapter).
     */
    public static String spawnTrackedEntityByIdFromPlayer(Object playerObj, String entityId, float speed) {
        return spawnTrackedEntityByIdFromPlayer(playerObj, entityId, speed, null);
    }

    public static String spawnTrackedEntityByIdFromPlayer(Object playerObj, String entityId, float speed,
                                                          Integer lifeTicksOverride) {
        if (!(playerObj instanceof Player player) || entityId == null || entityId.isEmpty()) {
            return null;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return null;
        }
        // Same loud DefaultedRegistry pig-fallback guard as
        // spawnEntityByIdFromPlayer: unknown ids throw instead of degrading.
        ResourceLocation key;
        try {
            key = ResourceLocations.parse(entityId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed entity type id '" + entityId + "'", e);
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            throw new IllegalArgumentException(
                "Unknown entity type id '" + entityId + "' (resolves to " + key
                    + "; remember the modid namespace, e.g. ac:entity_coin_throwing)");
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
        if (type == null) {
            return null;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return null;
        }
        entity.moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYRot(), player.getXRot());
        if (speed > 0.0F) {
            entity.setDeltaMovement(player.getLookAngle().normalize().scale(speed));
        }
        if (entity instanceof Projectile projectile) {
            projectile.setOwner(player);
        }
        // Mirror spawnEntityByIdFromPlayer's ScriptedEffectEntity handling:
        // without the owner the client-side orbit hook (OwnerOrbitEffectHook)
        // bails and the entity stays at its spawn point inside the player.
        if (entity instanceof ScriptedEffectEntity scriptedEffect) {
            scriptedEffect.setOwnerPlayer(player);
            scriptedEffect.setPos(player.getX(), player.getY() + 1.0D, player.getZ());
            if (lifeTicksOverride != null && lifeTicksOverride > 0) {
                scriptedEffect.setLifeTicksOverride(lifeTicksOverride);
            }
        }
        return level.addFreshEntity(entity) ? entity.getStringUUID() : null;
    }
}
