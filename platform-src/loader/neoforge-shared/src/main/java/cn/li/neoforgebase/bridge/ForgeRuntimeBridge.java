package cn.li.neoforgebase.bridge;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ForgeRuntimeBridge {
    private ForgeRuntimeBridge() {
    }

    public static boolean postEvent(Event event) {
        NeoForge.EVENT_BUS.post(event);
        return event instanceof ICancellableEvent cancellable && cancellable.isCanceled();
    }
}
