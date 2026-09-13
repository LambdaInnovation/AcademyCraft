package cn.li.neoforge262.event;

import cn.li.mcbase.datagen.DataGeneratorInterop;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * NeoForge 26.2 datagen wiring.
 * {@link GatherDataEvent} is abstract — register Client/Server listeners on the mod bus.
 * EventBusSubscriber.Bus enum was removed; prefer explicit {@link IEventBus} registration.
 */
public final class ForgeModBusListener {
    private ForgeModBusListener() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(GatherDataEvent.Client.class, ForgeModBusListener::onGatherDataClient);
        modBus.addListener(GatherDataEvent.Server.class, ForgeModBusListener::onGatherDataServer);
    }

    private static void onGatherDataClient(GatherDataEvent.Client event) {
        invokeGatherData(event);
    }

    private static void onGatherDataServer(GatherDataEvent.Server event) {
        invokeGatherData(event);
    }

    private static void invokeGatherData(GatherDataEvent event) {
        DataGeneratorInterop.invoke(
            "[academy] Error invoking Clojure DataGenerator handler: ",
            "cn.li.neoforge262.datagen.setup",
            "static-gather-data",
            event);
    }
}
