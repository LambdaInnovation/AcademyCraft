package cn.li.neoforgebase.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Dist lookup compatible with NeoForge 1.21.1 ({@code dist} field) and 26.2
 * ({@code getDist()}). Uses reflective access so one shared class compiles on both.
 */
public final class DistAccess {
    private DistAccess() {
    }

    public static Dist current() {
        try {
            Method getDist = FMLEnvironment.class.getMethod("getDist");
            return (Dist) getDist.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // fall through to field
        }
        try {
            Field dist = FMLEnvironment.class.getField("dist");
            return (Dist) dist.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to resolve FMLEnvironment dist", e);
        }
    }

    public static boolean isClient() {
        return current() == Dist.CLIENT;
    }

    public static boolean isDedicatedServer() {
        return current() == Dist.DEDICATED_SERVER;
    }
}
