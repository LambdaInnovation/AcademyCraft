package cn.li.mc262.runtime;

import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mcmod.ModId;
import cn.li.mcver.ResourceLocations;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
                Identifier id = particleType.contains(":")
                    ? ResourceLocations.parse(particleType)
                    : ResourceLocations.of(ModId.ID, particleType.replace('-', '_'));
                ParticleType<?> dynamicType = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
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

    public static boolean spawnEntityByIdFromPlayer(Object playerObj, String entityId, float speed,
                                                    Integer lifeTicksOverride) {
        Entity entity = spawnEntityInternal(playerObj, entityId, speed, lifeTicksOverride);
        return entity != null;
    }

    public static String spawnTrackedEntityByIdFromPlayer(Object playerObj, String entityId, float speed) {
        return spawnTrackedEntityByIdFromPlayer(playerObj, entityId, speed, null);
    }

    public static String spawnTrackedEntityByIdFromPlayer(Object playerObj, String entityId, float speed,
                                                          Integer lifeTicksOverride) {
        Entity entity = spawnEntityInternal(playerObj, entityId, speed, lifeTicksOverride);
        return entity == null ? null : entity.getStringUUID();
    }

    private static Entity spawnEntityInternal(Object playerObj, String entityId, float speed,
                                              Integer lifeTicksOverride) {
        if (!(playerObj instanceof Player player) || entityId == null || entityId.isEmpty()) {
            return null;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return null;
        }
        Identifier key;
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
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
        if (type == null) {
            return null;
        }
        Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            return null;
        }
        entity.snapTo(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYRot(), player.getXRot());
        if (speed > 0.0F) {
            Vec3 look = player.getLookAngle().normalize().scale(speed);
            entity.setDeltaMovement(look);
        }
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
        return level.addFreshEntity(entity) ? entity : null;
    }
}
