package cn.li.forge1201.entity;

import cn.li.forge1201.AcademyCraft1201;
import cn.li.mc1201.entity.ScriptedBlockBodyEntity;
import cn.li.mc1201.entity.ScriptedEffectEntity;
import cn.li.mc1201.entity.ScriptedMarkerEntity;
import cn.li.mc1201.entity.ScriptedMobEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ModEntities {
    private ModEntities() {
    }

    static {
        ScriptedEntitySpecAccess.install(new ScriptedEntitySpecAccess.Accessor() {
            @Override
            public <T extends Entity> EntityType<T> requireEntityType(String registryName, Class<T> expectedClass) {
                return ModEntities.requireEntityType(registryName, expectedClass);
            }

            @Override
            public Class<? extends Entity> resolveEntityClassByKind(String entityKind) {
                return ModEntities.resolveEntityClassByKind(entityKind);
            }

            @Override
            public ScriptedEffectSpec getScriptedEffectSpec(EntityType<?> entityType) {
                return ModEntities.getScriptedEffectSpec(entityType);
            }

            @Override
            public ScriptedRaySpec getScriptedRaySpec(EntityType<?> entityType) {
                return ModEntities.getScriptedRaySpec(entityType);
            }

            @Override
            public ScriptedMarkerSpec getScriptedMarkerSpec(EntityType<?> entityType) {
                return ModEntities.getScriptedMarkerSpec(entityType);
            }

            @Override
            public ScriptedProjectileSpec getScriptedProjectileSpec(EntityType<?> entityType) {
                return ModEntities.getScriptedProjectileSpec(entityType);
            }

            @Override
            public ScriptedBlockBodySpec getScriptedBlockBodySpec(EntityType<?> entityType) {
                return ModEntities.getScriptedBlockBodySpec(entityType);
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

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AcademyCraft1201.MODID);

    private static final Map<String, RegistryObject<EntityType<?>>> REGISTERED_ENTITY_TYPES = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedProjectileSpec> SCRIPTED_PROJECTILE_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedEffectSpec> SCRIPTED_EFFECT_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedRaySpec> SCRIPTED_RAY_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedMarkerSpec> SCRIPTED_MARKER_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ScriptedBlockBodySpec> SCRIPTED_BLOCK_BODY_SPECS = new ConcurrentHashMap<>();

    public static RegistryObject<EntityType<?>> register(String registryName, Supplier<? extends EntityType<?>> supplier) {
        RegistryObject<EntityType<?>> entry = ENTITY_TYPES.register(registryName, () -> (EntityType<?>) supplier.get());
        REGISTERED_ENTITY_TYPES.put(registryName, entry);
        return entry;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityType<T> getEntityType(String registryName, Class<T> expectedClass) {
        RegistryObject<EntityType<?>> entry = REGISTERED_ENTITY_TYPES.get(registryName);
        if (entry == null) {
            return null;
        }
        if (!entry.isPresent()) {
            return null;
        }
        return (EntityType<T>) entry.get();
    }

    public static <T extends Entity> EntityType<T> requireEntityType(String registryName, Class<T> expectedClass) {
        EntityType<T> type = getEntityType(registryName, expectedClass);
        if (type == null) {
            throw new IllegalStateException("Entity type is not registered: " + registryName);
        }
        return type;
    }

    public static Class<? extends Entity> resolveEntityClassByKind(String entityKind) {
        return switch (String.valueOf(entityKind)) {
            case "scripted-projectile" -> ScriptedProjectileEntity.class;
            case "scripted-effect" -> ScriptedEffectEntity.class;
            case "scripted-ray" -> ScriptedRayEntity.class;
            case "scripted-marker" -> ScriptedMarkerEntity.class;
            case "scripted-block-body" -> ScriptedBlockBodyEntity.class;
            case "scripted-mob" -> ScriptedMobEntity.class;
            default -> null;
        };
    }

    public static void registerScriptedProjectileSpec(String registryName,
                                                      String defaultItemId,
                                                      double gravity,
                                                      double damage,
                                                      double pickupDistanceSqr,
                                                      boolean dropItemOnDiscard,
                                                      String onHitBlockHook,
                                                      String onHitEntityHook,
                                                      String onAnchoredTickHook,
                                                      String onAnchoredHurtHook) {
        SCRIPTED_PROJECTILE_SPECS.put(
                registryName,
                new ScriptedProjectileSpec(
                        defaultItemId,
                        gravity,
                        damage,
                        pickupDistanceSqr,
                        dropItemOnDiscard,
                        onHitBlockHook,
                        onHitEntityHook,
                        onAnchoredTickHook,
                        onAnchoredHurtHook
                )
        );
    }

    public static ScriptedProjectileSpec getScriptedProjectileSpec(String registryName) {
        return SCRIPTED_PROJECTILE_SPECS.get(registryName);
    }

    public static ScriptedProjectileSpec getScriptedProjectileSpec(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return null;
        }
        return SCRIPTED_PROJECTILE_SPECS.get(key.getPath());
    }

    public static void registerScriptedEffectSpec(String registryName,
                                                  int lifeTicks,
                                                  boolean followOwner,
                                                  String effectHook) {
        registerScriptedEffectSpec(registryName, lifeTicks, followOwner, "effect-billboard", effectHook, Collections.emptyMap());
        }

        public static void registerScriptedEffectSpec(String registryName,
                              int lifeTicks,
                              boolean followOwner,
                              String rendererId,
                              String effectHook) {
        registerScriptedEffectSpec(registryName, lifeTicks, followOwner, rendererId, effectHook, Collections.emptyMap());
    }

    public static void registerScriptedEffectSpec(String registryName,
                              int lifeTicks,
                              boolean followOwner,
                              String rendererId,
                              String effectHook,
                              Map<String, Object> hookParams) {
        SCRIPTED_EFFECT_SPECS.put(
                registryName,
            new ScriptedEffectSpec(lifeTicks, followOwner, rendererId, effectHook, hookParams)
        );
    }

    public static ScriptedEffectSpec getScriptedEffectSpec(String registryName) {
        return SCRIPTED_EFFECT_SPECS.get(registryName);
    }

    public static ScriptedEffectSpec getScriptedEffectSpec(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return null;
        }
        return SCRIPTED_EFFECT_SPECS.get(key.getPath());
    }

    public static Set<String> getScriptedEffectRegistryNames() {
        return Collections.unmodifiableSet(SCRIPTED_EFFECT_SPECS.keySet());
    }

    public static void registerScriptedRaySpec(String registryName,
                                               int lifeTicks,
                                               double length,
                                               double blendInMs,
                                               double blendOutMs,
                               double innerWidth,
                               double outerWidth,
                               double glowWidth,
                               int startColor,
                               int endColor,
                                               String rendererId,
                                               String hookId,
                                               Map<String, Object> hookParams) {
        SCRIPTED_RAY_SPECS.put(
                registryName,
            new ScriptedRaySpec(
                lifeTicks,
                length,
                blendInMs,
                blendOutMs,
                innerWidth,
                outerWidth,
                glowWidth,
                startColor,
                endColor,
                rendererId,
                hookId,
                hookParams)
        );
    }

    public static ScriptedRaySpec getScriptedRaySpec(String registryName) {
        return SCRIPTED_RAY_SPECS.get(registryName);
    }

    public static ScriptedRaySpec getScriptedRaySpec(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return null;
        }
        return SCRIPTED_RAY_SPECS.get(key.getPath());
    }

    public static Set<String> getScriptedRayRegistryNames() {
        return Collections.unmodifiableSet(SCRIPTED_RAY_SPECS.keySet());
    }

    public static void registerScriptedMarkerSpec(String registryName,
                                                  int lifeTicks,
                                                  boolean followTarget,
                                                  boolean ignoreDepth,
                              boolean available,
                                                  String rendererId,
                                                  String hookId) {
        SCRIPTED_MARKER_SPECS.put(
                registryName,
            new ScriptedMarkerSpec(lifeTicks, followTarget, ignoreDepth, available, rendererId, hookId)
        );
    }

    public static ScriptedMarkerSpec getScriptedMarkerSpec(String registryName) {
        return SCRIPTED_MARKER_SPECS.get(registryName);
    }

    public static ScriptedMarkerSpec getScriptedMarkerSpec(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return null;
        }
        return SCRIPTED_MARKER_SPECS.get(key.getPath());
    }

    public static Set<String> getScriptedMarkerRegistryNames() {
        return Collections.unmodifiableSet(SCRIPTED_MARKER_SPECS.keySet());
    }

    public static void registerScriptedBlockBodySpec(String registryName,
                                                     String defaultBlockId,
                                                     double gravity,
                                                     double damage,
                                                     boolean placeWhenCollide,
                                                     String rendererId,
                                                     String hookId,
                                                     String behaviorId) {
        registerScriptedBlockBodySpec(registryName, defaultBlockId, gravity, damage,
                placeWhenCollide, rendererId, hookId, behaviorId, 1.0D);
    }

    public static void registerScriptedBlockBodySpec(String registryName,
                                                     String defaultBlockId,
                                                     double gravity,
                                                     double damage,
                                                     boolean placeWhenCollide,
                                                     String rendererId,
                                                     String hookId,
                                                     String behaviorId,
                                                     double drag) {
        SCRIPTED_BLOCK_BODY_SPECS.put(
                registryName,
                new ScriptedBlockBodySpec(defaultBlockId, gravity, damage, placeWhenCollide, rendererId, hookId, behaviorId, drag)
        );
    }

    public static ScriptedBlockBodySpec getScriptedBlockBodySpec(String registryName) {
        return SCRIPTED_BLOCK_BODY_SPECS.get(registryName);
    }

    public static ScriptedBlockBodySpec getScriptedBlockBodySpec(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return null;
        }
        return SCRIPTED_BLOCK_BODY_SPECS.get(key.getPath());
    }

    public static Set<String> getScriptedBlockBodyRegistryNames() {
        return Collections.unmodifiableSet(SCRIPTED_BLOCK_BODY_SPECS.keySet());
    }

    public static boolean registerScriptedEffectHookClass(String hookId, String className) {
        return ScriptedEffectHooks.registerByClassName(hookId, className);
    }

    public static boolean registerScriptedRayHookClass(String hookId, String className) {
        return ScriptedRayHooks.registerByClassName(hookId, className);
    }

    public static boolean registerScriptedMarkerHookClass(String hookId, String className) {
        return ScriptedMarkerHooks.registerByClassName(hookId, className);
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    /**
     * Register PathfinderMob default attributes for a scripted-mob entity type.
     * Called from Clojure entity-attributes event handler to avoid clj-kondo
     * import resolution issues with Minecraft internal classes.
     *
     * <p>The unchecked cast is safe: only :scripted-mob entities (which extend
     * PathfinderMob → LivingEntity) reach this call site.</p>
     */
    @SuppressWarnings("unchecked")
    public static void registerMobDefaultAttributes(
            net.minecraftforge.event.entity.EntityAttributeCreationEvent event,
            EntityType<?> entityType) {
        event.put((EntityType<? extends LivingEntity>) entityType,
                  PathfinderMob.createMobAttributes().build());
    }
}
