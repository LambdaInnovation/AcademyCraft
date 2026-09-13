package cn.li.mcbase.block;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shared dynamic BlockState property registry/context helper.
 *
 * <p>Stores per-block-class property lists by block-id while also supporting
 * a thread-local init context for constructor-time state-definition wiring.</p>
 */
public final class DynamicBlockPropertyRegistry {

    private static final class InitContext {
        final Class<?> ownerClass;
        final String blockId;
        final List<Property<?>> properties;

        InitContext(Class<?> ownerClass, String blockId, List<Property<?>> properties) {
            this.ownerClass = ownerClass;
            this.blockId = blockId;
            this.properties = properties;
        }
    }

    private static final Map<Class<?>, Map<String, List<Property<?>>>> PROPERTIES = new ConcurrentHashMap<>();
    private static final ThreadLocal<InitContext> INIT_CONTEXT = new ThreadLocal<>();

    private DynamicBlockPropertyRegistry() {
    }

    public static <T> T withInitContext(Class<?> ownerClass,
                                        String blockId,
                                        List<Property<?>> properties,
                                        Supplier<T> supplier) {
        List<Property<?>> safeProperties = properties != null ? properties : Collections.emptyList();
        INIT_CONTEXT.set(new InitContext(ownerClass, blockId, safeProperties));
        try {
            return supplier.get();
        } finally {
            INIT_CONTEXT.remove();
        }
    }

    public static List<Property<?>> resolveForDefinition(Class<?> ownerClass, String blockId) {
        InitContext ctx = INIT_CONTEXT.get();
        if (ctx != null && ctx.ownerClass == ownerClass) {
            List<Property<?>> properties = ctx.properties != null ? ctx.properties : Collections.emptyList();
            if (ctx.blockId != null) {
                getOwnerProperties(ownerClass).put(ctx.blockId, properties);
            }
            return properties;
        }
        if (blockId == null) {
            return Collections.emptyList();
        }
        return getOwnerProperties(ownerClass).getOrDefault(blockId, Collections.emptyList());
    }

    public static boolean hasDynamicProperties(Class<?> ownerClass, String blockId) {
        InitContext ctx = INIT_CONTEXT.get();
        if (ctx != null && ctx.ownerClass == ownerClass) {
            return ctx.properties != null && !ctx.properties.isEmpty();
        }
        if (blockId == null) {
            return false;
        }
        List<Property<?>> properties = getOwnerProperties(ownerClass).getOrDefault(blockId, Collections.emptyList());
        return properties != null && !properties.isEmpty();
    }

    private static Map<String, List<Property<?>>> getOwnerProperties(Class<?> ownerClass) {
        return PROPERTIES.computeIfAbsent(ownerClass, ignored -> new ConcurrentHashMap<>());
    }
}
