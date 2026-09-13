package cn.li.neoforgebase.bridge;

import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Typed NeoForge event mutation helpers for Clojure AOT / checkClojure.
 * Cancel APIs live on {@link ICancellableEvent} and specific interact subclasses,
 * not on the abstract {@link PlayerInteractEvent} / {@link Event} types.
 */
public final class EventInterop {
    private EventInterop() {
    }

    public static void cancelPlayerInteract(PlayerInteractEvent evt, InteractionResult result) {
        setCancellationResult(evt, result);
        if (evt instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    public static void cancelEvent(Event evt) {
        if (evt instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    private static void setCancellationResult(PlayerInteractEvent evt, InteractionResult result) {
        if (evt instanceof PlayerInteractEvent.RightClickBlock rightClickBlock) {
            rightClickBlock.setCancellationResult(result);
        } else if (evt instanceof PlayerInteractEvent.RightClickItem rightClickItem) {
            rightClickItem.setCancellationResult(result);
        } else if (evt instanceof PlayerInteractEvent.EntityInteract entityInteract) {
            entityInteract.setCancellationResult(result);
        } else if (evt instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
            specific.setCancellationResult(result);
        }
        // LeftClickBlock is cancellable but has no cancellation InteractionResult.
    }
}
