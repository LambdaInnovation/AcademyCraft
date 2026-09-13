package cn.li.neoforge1211.bridge;

import net.minecraft.client.Minecraft;

/** 1.21 client timing (Minecraft#getFrameTime removed). */
public final class ClientTimeInterop {
    private ClientTimeInterop() {
    }

    public static float getFrameTime(Minecraft minecraft) {
        return minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
