package cn.li.fabric1201.entity;

import cn.li.mcver.ResourceLocations;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import cn.li.mcmod.ModId;
import cn.li.mc1201.entity.ScriptedBlockBodyEntity;
import cn.li.mc1201.entity.ScriptedEffectEntity;
import cn.li.mc1201.entity.ScriptedMarkerEntity;
import cn.li.mc1201.entity.ScriptedProjectileEntity;
import cn.li.mc1201.entity.ScriptedRayEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric entity type registration.
 *
 * Registers the five scripted entity types (projectile, effect, ray, marker, block-body)
 * using Fabric's Registry system.
 */
public final class FabricEntities {
    private static final String MOD_ID = ModId.ID;
    private static final Map<String, EntityType<?>> ENTITY_TYPES = new HashMap<>();

    private FabricEntities() {
    }

    public static void registerEntities() {
        // Register projectile entity
        EntityType<ScriptedProjectileEntity> projectileType = EntityType.Builder.of(
                ScriptedProjectileEntity::new,
                MobCategory.MISC
        )
                .sized(0.5f, 0.5f)
                .build(MOD_ID + ":scripted-projectile");
        registerEntityType("scripted-projectile", projectileType);

        // Register effect entity
        EntityType<ScriptedEffectEntity> effectType = EntityType.Builder.of(
                ScriptedEffectEntity::new,
                MobCategory.MISC
        )
                .sized(0.5f, 0.5f)
                .noSummon()
                .noSave()
                .build(MOD_ID + ":scripted-effect");
        registerEntityType("scripted-effect", effectType);

        // Register ray entity
        EntityType<ScriptedRayEntity> rayType = EntityType.Builder.of(
                ScriptedRayEntity::new,
                MobCategory.MISC
        )
                .sized(0.5f, 0.5f)
                .noSummon()
                .noSave()
                .build(MOD_ID + ":scripted-ray");
        registerEntityType("scripted-ray", rayType);

        // Register marker entity
        EntityType<ScriptedMarkerEntity> markerType = EntityType.Builder.of(
                ScriptedMarkerEntity::new,
                MobCategory.MISC
        )
                .sized(0.1f, 0.1f)
                .noSummon()
                .noSave()
                .build(MOD_ID + ":scripted-marker");
        registerEntityType("scripted-marker", markerType);

        // Register block-body entity
        EntityType<ScriptedBlockBodyEntity> blockBodyType = EntityType.Builder.of(
                ScriptedBlockBodyEntity::new,
                MobCategory.MISC
        )
                .sized(1.0f, 1.0f)
                .build(MOD_ID + ":scripted-block-body");
        registerEntityType("scripted-block-body", blockBodyType);
    }

        private static <E extends net.minecraft.world.entity.Entity> void registerEntityType(
            String id, EntityType<E> entityType) {
        ResourceLocation location = ResourceLocations.of(MOD_ID, id);
        @SuppressWarnings("unchecked")
        EntityType<E> registered = (EntityType<E>) Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                location,
                entityType
        );
        ENTITY_TYPES.put(id, registered);
        // Also register with the shared accessor
        FabricScriptedEntityAccess.registerEntityType(location.toString(), registered);
    }

    public static EntityType<?> getEntityType(String id) {
        return ENTITY_TYPES.get(id);
    }
}
