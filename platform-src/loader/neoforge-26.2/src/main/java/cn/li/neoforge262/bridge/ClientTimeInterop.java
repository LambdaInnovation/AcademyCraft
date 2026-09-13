package cn.li.neoforge262.bridge;

import net.minecraft.client.Minecraft;

/** 26.2 client timing (Minecraft#getTimer removed; use DeltaTracker). */
public final class ClientTimeInterop {
    private ClientTimeInterop() {
    }

    public static float getFrameTime(Minecraft minecraft) {
        return minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
