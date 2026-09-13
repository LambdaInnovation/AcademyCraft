package cn.li.fabric1201.entity;

import cn.li.mc1201.entity.ScriptedBlockBodyEntity;
import cn.li.mc1201.entity.ScriptedEffectEntity;
import cn.li.mc1201.entity.ScriptedMarkerEntity;
import cn.li.mc1201.entity.ScriptedProjectileEntity;
import cn.li.mc1201.entity.ScriptedRayEntity;
import cn.li.mcbase.entity.hook.effect.ScriptedEffectHooks;
import cn.li.mcbase.entity.hook.marker.ScriptedMarkerHooks;
import cn.li.mcbase.entity.hook.ray.ScriptedRayHooks;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedProjectileSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric-side installer for shared scripted entity access.
 *
 * <p>Provides shared spec lookup and entity type lookup storage without
 * introducing Forge-specific classes.</p>
 */
public final class FabricScriptedEntityAccess {
    private static final String EFFECT_KIND_KEY = "scripted-effect";
    private static final String RAY_KIND_KEY = "scripted-ray";
    private static final String MARKER_KIND_KEY = "scripted-marker";
    private static final String BLOCK_BODY_KIND_KEY = "scripted-block-body";

    private static final Map<String, EntityType<?>> REGISTERED_ENTITY_TYPES = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedProjectileSpec> SCRIPTED_PROJECTILE_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedEffectSpec> SCRIPTED_EFFECT_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedRaySpec> SCRIPTED_RAY_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedMarkerSpec> SCRIPTED_MARKER_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedBlockBodySpec> SCRIPTED_BLOCK_BODY_SPECS = new ConcurrentHashMap<>();

    private FabricScriptedEntityAccess() {
    }

    public static void install() {
        ScriptedEntitySpecAccess.install(new ScriptedEntitySpecAccess.Accessor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends Entity> EntityType<T> requireEntityType(String registryName, Class<T> expectedClass) {
                EntityType<?> type = REGISTERED_ENTITY_TYPES.get(registryName);
                if (type == null) {
                    throw new IllegalStateException("Fabric entity type is not registered: " + registryName);
                }
                return (EntityType<T>) type;
            }

            @Override
            public Class<? extends Entity> resolveEntityClassByKind(String entityKind) {
                if (entityKind == null || entityKind.isEmpty()) {
                    return null;
                }
                return switch (entityKind) {
                    case "scripted-projectile" -> ScriptedProjectileEntity.class;
                    case "scripted-effect" -> ScriptedEffectEntity.class;
                    case "scripted-ray" -> ScriptedRayEntity.class;
                    case "scripted-marker" -> ScriptedMarkerEntity.class;
                    case "scripted-block-body" -> ScriptedBlockBodyEntity.class;
                    default -> null;
                };
            }

            @Override
            public ScriptedEffectSpec getScriptedEffectSpec(EntityType<?> entityType) {
                return getWithKindFallback(SCRIPTED_EFFECT_SPECS, registryName(entityType), EFFECT_KIND_KEY);
            }

            @Override
            public ScriptedRaySpec getScriptedRaySpec(EntityType<?> entityType) {
                return getWithKindFallback(SCRIPTED_RAY_SPECS, registryName(entityType), RAY_KIND_KEY);
            }

            @Override
            public ScriptedMarkerSpec getScriptedMarkerSpec(EntityType<?> entityType) {
                return getWithKindFallback(SCRIPTED_MARKER_SPECS, registryName(entityType), MARKER_KIND_KEY);
            }

            @Override
            public ScriptedProjectileSpec getScriptedProjectileSpec(EntityType<?> entityType) {
                return SCRIPTED_PROJECTILE_SPECS.get(registryName(entityType));
            }

            @Override
            public ScriptedBlockBodySpec getScriptedBlockBodySpec(EntityType<?> entityType) {
                return getWithKindFallback(SCRIPTED_BLOCK_BODY_SPECS, registryName(entityType), BLOCK_BODY_KIND_KEY);
            }

            @Override
            public boolean registerScriptedEffectHookClass(String hookId, String className) {
                return ScriptedEffectHooks.registerByClassName(hookId, className);
            }

            @Override
            public boolean registerScriptedRayHookClass(String hookId, String className) {
                return ScriptedRayHooks.registerByClassName(hookId, className);
            }

            @Override
            public boolean registerScriptedMarkerHookClass(String hookId, String className) {
                return ScriptedMarkerHooks.registerByClassName(hookId, className);
            }
        });
    }

    public static void registerEntityType(String registryName, EntityType<?> entityType) {
        if (registryName == null || registryName.isEmpty() || entityType == null) {
            return;
        }
        REGISTERED_ENTITY_TYPES.put(registryName, entityType);
    }

    public static void registerScriptedProjectileSpec(String registryName, ScriptedProjectileSpec spec) {
        if (registryName != null && !registryName.isEmpty() && spec != null) {
            SCRIPTED_PROJECTILE_SPECS.put(registryName, spec);
        }
    }

    public static void registerScriptedEffectSpec(String registryName, ScriptedEffectSpec spec) {
        if (registryName != null && !registryName.isEmpty() && spec != null) {
            SCRIPTED_EFFECT_SPECS.put(registryName, spec);
            SCRIPTED_EFFECT_SPECS.putIfAbsent(EFFECT_KIND_KEY, spec);
        }
    }

    public static void registerScriptedRaySpec(String registryName, ScriptedRaySpec spec) {
        if (registryName != null && !registryName.isEmpty() && spec != null) {
            SCRIPTED_RAY_SPECS.put(registryName, spec);
            SCRIPTED_RAY_SPECS.putIfAbsent(RAY_KIND_KEY, spec);
        }
    }

    public static void registerScriptedMarkerSpec(String registryName, ScriptedMarkerSpec spec) {
        if (registryName != null && !registryName.isEmpty() && spec != null) {
            SCRIPTED_MARKER_SPECS.put(registryName, spec);
            SCRIPTED_MARKER_SPECS.putIfAbsent(MARKER_KIND_KEY, spec);
        }
    }

    public static void registerScriptedBlockBodySpec(String registryName, ScriptedBlockBodySpec spec) {
        if (registryName != null && !registryName.isEmpty() && spec != null) {
            SCRIPTED_BLOCK_BODY_SPECS.put(registryName, spec);
            SCRIPTED_BLOCK_BODY_SPECS.putIfAbsent(BLOCK_BODY_KIND_KEY, spec);
        }
    }

    private static <T> T getWithKindFallback(Map<String, T> map, String key, String fallbackKey) {
        if (key != null) {
            T value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return map.get(fallbackKey);
    }

    private static String registryName(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return key == null ? null : key.getPath();
    }
}
