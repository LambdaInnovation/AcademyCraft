package cn.li.mcbase.entity.hook;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Shared per-tick hook contract for scripted entities.
 *
 * <p>Both sides default to no-op; a hook implements the side it needs.
 * Server-authoritative hooks (e.g. {@code OwnerOrbitEffectHook}) drive the
 * entity position on the server-owned instance and let vanilla entity
 * tracking sync it to clients — the client never re-computes it.
 *
 * @param <E> entity type handled by this hook
 */
public interface ClientEntityHook<E extends Entity> {
    default void onClientTick(E entity, ClientLevel level) {
    }

    default void onServerTick(E entity, Level level) {
    }
}
