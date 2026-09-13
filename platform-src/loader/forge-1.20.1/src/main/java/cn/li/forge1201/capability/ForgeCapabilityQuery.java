package cn.li.forge1201.capability;

import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * Read-only capability registry queries for Forge capability handling.
 */
public final class ForgeCapabilityQuery {

    private ForgeCapabilityQuery() {
    }

    @Nullable
    public static String getKey(Capability<?> cap) {
        return CapabilityRegistry.getKey(cap);
    }
}