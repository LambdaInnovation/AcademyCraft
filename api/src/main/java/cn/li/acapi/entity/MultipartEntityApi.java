package cn.li.acapi.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cross-loader registration API for custom multipart entity implementations.
 *
 * <p>Resolvers are process-static integration metadata. They are evaluated by
 * descending priority and then by id, so resolution is deterministic regardless
 * of mod loading order. A resolver should return an immediate parent only when it
 * recognizes the supplied object, and {@code null} otherwise.</p>
 */
public final class MultipartEntityApi {

    @FunctionalInterface
    public interface ParentResolver {
        Object findParent(Object entityPart);
    }

    @FunctionalInterface
    public interface ParentValidator {
        boolean isValid(Object parent);
    }

    private record ResolverEntry(String id, int priority, ParentResolver resolver) {}

    private static final Comparator<ResolverEntry> RESOLVER_ORDER =
        Comparator.comparingInt(ResolverEntry::priority)
            .reversed()
            .thenComparing(ResolverEntry::id);

    private static final Map<String, ResolverEntry> RESOLVERS = new HashMap<>();
    private static volatile List<ResolverEntry> resolverSnapshot = List.of();

    private MultipartEntityApi() {}

    /**
     * Register or replace a resolver under a stable, namespaced id.
     *
     * @param id stable id such as {@code "examplemod:multipart_entity"}
     * @param priority higher values run first
     * @param resolver resolver that returns an immediate parent or {@code null}
     */
    public static synchronized void registerParentResolver(
        String id,
        int priority,
        ParentResolver resolver
    ) {
        if (id == null || id.isBlank() || id.indexOf(':') <= 0 || id.endsWith(":")) {
            throw new IllegalArgumentException("Multipart parent resolver id must be namespaced");
        }
        Objects.requireNonNull(resolver, "resolver");
        RESOLVERS.put(id, new ResolverEntry(id, priority, resolver));
        rebuildSnapshot();
    }

    /**
     * Remove a resolver previously registered under {@code id}.
     *
     * @return {@code true} when a resolver was removed
     */
    public static synchronized boolean unregisterParentResolver(String id) {
        boolean removed = RESOLVERS.remove(id) != null;
        if (removed) {
            rebuildSnapshot();
        }
        return removed;
    }

    /**
     * Resolve an immediate parent using the current immutable resolver snapshot.
     *
     * <p>Failures from optional integrations are isolated so one incompatible Mod
     * cannot prevent later resolvers from handling the same entity. JVM-fatal
     * errors are deliberately not swallowed.</p>
     */
    public static Object resolveParent(Object entityPart) {
        return resolveParent(entityPart, Object.class);
    }

    /**
     * Resolve an immediate parent that is assignable to {@code expectedParentType}.
     * Invalid non-null results are skipped so they cannot mask a later compatible
     * resolver.
     */
    public static Object resolveParent(Object entityPart, Class<?> expectedParentType) {
        Objects.requireNonNull(expectedParentType, "expectedParentType");
        return resolveParent(entityPart, expectedParentType::isInstance);
    }

    /**
     * Resolve an immediate parent accepted by {@code parentValidator}.
     */
    public static Object resolveParent(Object entityPart, ParentValidator parentValidator) {
        if (entityPart == null) {
            return null;
        }
        Objects.requireNonNull(parentValidator, "parentValidator");
        for (ResolverEntry entry : resolverSnapshot) {
            try {
                Object parent = entry.resolver().findParent(entityPart);
                if (parent != null
                    && parent != entityPart
                    && parentValidator.isValid(parent)) {
                    return parent;
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Optional compatibility resolvers must not break combat handling.
            }
        }
        return null;
    }

    /**
     * Return resolver ids in their effective evaluation order.
     */
    public static List<String> registeredResolverIds() {
        return resolverSnapshot.stream().map(ResolverEntry::id).toList();
    }

    private static void rebuildSnapshot() {
        ArrayList<ResolverEntry> entries = new ArrayList<>(RESOLVERS.values());
        entries.sort(RESOLVER_ORDER);
        resolverSnapshot = List.copyOf(entries);
    }
}
