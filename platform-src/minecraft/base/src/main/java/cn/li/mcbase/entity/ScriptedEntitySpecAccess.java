package cn.li.mcbase.entity;

import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedProjectileSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Shared indirection for scripted entity type/spec access.
 *
 * <p>Platform modules install a loader-specific accessor during bootstrap, while
 * shared/portable entity logic reads through this class to avoid hard dependencies
 * on a concrete platform registry holder.</p>
 */
public final class ScriptedEntitySpecAccess {

    private ScriptedEntitySpecAccess() {
    }

    public interface Accessor {
        <T extends Entity> EntityType<T> requireEntityType(String registryName, Class<T> expectedClass);

        Class<? extends Entity> resolveEntityClassByKind(String entityKind);

        ScriptedEffectSpec getScriptedEffectSpec(EntityType<?> entityType);

        ScriptedRaySpec getScriptedRaySpec(EntityType<?> entityType);

        ScriptedMarkerSpec getScriptedMarkerSpec(EntityType<?> entityType);

        ScriptedProjectileSpec getScriptedProjectileSpec(EntityType<?> entityType);

        ScriptedBlockBodySpec getScriptedBlockBodySpec(EntityType<?> entityType);

        boolean registerScriptedEffectHookClass(String hookId, String className);

        boolean registerScriptedRayHookClass(String hookId, String className);

        boolean registerScriptedMarkerHookClass(String hookId, String className);
    }

    private static final Accessor UNINITIALIZED = new Accessor() {
        private IllegalStateException uninitialized() {
            return new IllegalStateException("ScriptedEntitySpecAccess accessor is not installed");
        }

        @Override
        public <T extends Entity> EntityType<T> requireEntityType(String registryName, Class<T> expectedClass) {
            throw uninitialized();
        }

        @Override
        public Class<? extends Entity> resolveEntityClassByKind(String entityKind) {
            throw uninitialized();
        }

        @Override
        public ScriptedEffectSpec getScriptedEffectSpec(EntityType<?> entityType) {
            throw uninitialized();
        }

        @Override
        public ScriptedRaySpec getScriptedRaySpec(EntityType<?> entityType) {
            throw uninitialized();
        }

        @Override
        public ScriptedMarkerSpec getScriptedMarkerSpec(EntityType<?> entityType) {
            throw uninitialized();
        }

        @Override
        public ScriptedProjectileSpec getScriptedProjectileSpec(EntityType<?> entityType) {
            throw uninitialized();
        }

        @Override
        public ScriptedBlockBodySpec getScriptedBlockBodySpec(EntityType<?> entityType) {
            throw uninitialized();
        }

        @Override
        public boolean registerScriptedEffectHookClass(String hookId, String className) {
            throw uninitialized();
        }

        @Override
        public boolean registerScriptedRayHookClass(String hookId, String className) {
            throw uninitialized();
        }

        @Override
        public boolean registerScriptedMarkerHookClass(String hookId, String className) {
            throw uninitialized();
        }
    };

    private static volatile Accessor accessor = UNINITIALIZED;

    public static void install(Accessor newAccessor) {
        accessor = newAccessor == null ? UNINITIALIZED : newAccessor;
    }

    public static <T extends Entity> EntityType<T> requireEntityType(String registryName, Class<T> expectedClass) {
        return accessor.requireEntityType(registryName, expectedClass);
    }

    public static Class<? extends Entity> resolveEntityClassByKind(String entityKind) {
        return accessor.resolveEntityClassByKind(entityKind);
    }

    public static ScriptedEffectSpec getScriptedEffectSpec(EntityType<?> entityType) {
        return accessor.getScriptedEffectSpec(entityType);
    }

    public static ScriptedRaySpec getScriptedRaySpec(EntityType<?> entityType) {
        return accessor.getScriptedRaySpec(entityType);
    }

    public static ScriptedMarkerSpec getScriptedMarkerSpec(EntityType<?> entityType) {
        return accessor.getScriptedMarkerSpec(entityType);
    }

    public static ScriptedProjectileSpec getScriptedProjectileSpec(EntityType<?> entityType) {
        return accessor.getScriptedProjectileSpec(entityType);
    }

    public static ScriptedBlockBodySpec getScriptedBlockBodySpec(EntityType<?> entityType) {
        return accessor.getScriptedBlockBodySpec(entityType);
    }

    public static boolean registerScriptedEffectHookClass(String hookId, String className) {
        return accessor.registerScriptedEffectHookClass(hookId, className);
    }

    public static boolean registerScriptedRayHookClass(String hookId, String className) {
        return accessor.registerScriptedRayHookClass(hookId, className);
    }

    public static boolean registerScriptedMarkerHookClass(String hookId, String className) {
        return accessor.registerScriptedMarkerHookClass(hookId, className);
    }
}