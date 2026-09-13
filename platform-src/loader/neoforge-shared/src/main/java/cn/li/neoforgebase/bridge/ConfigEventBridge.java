package cn.li.neoforgebase.bridge;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.function.Consumer;

/**
 * Bridge for registering Forge config event listeners without resorting to
 * Class.forName("...ModConfigEvent$Loading") strings that Loom cannot remap.
 */
public final class ConfigEventBridge {
    private ConfigEventBridge() {}

    public static void addConfigListeners(IEventBus modBus, Consumer<ModConfigEvent> listener) {
        modBus.addListener(EventPriority.NORMAL, false, ModConfigEvent.Loading.class,
                (ModConfigEvent.Loading e) -> listener.accept(e));
        modBus.addListener(EventPriority.NORMAL, false, ModConfigEvent.Reloading.class,
                (ModConfigEvent.Reloading e) -> listener.accept(e));
    }
}
