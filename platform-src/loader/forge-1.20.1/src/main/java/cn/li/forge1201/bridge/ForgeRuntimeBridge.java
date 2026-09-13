package cn.li.forge1201.bridge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

public final class ForgeRuntimeBridge {
    private ForgeRuntimeBridge() {
    }

    public static boolean postEvent(Event event) {
        return MinecraftForge.EVENT_BUS.post(event);
    }
}