package cn.li.neoforgebase.capability;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

import javax.annotation.Nullable;

/** Read-only capability registry queries for NeoForge capability handling. */
public final class ForgeCapabilityQuery {
    private ForgeCapabilityQuery() {
    }

    @Nullable
    public static String getKey(BlockCapability<?, ?> cap) {
        return CapabilityKeyIndex.getKey(cap);
    }

    @Nullable
    public static String getKey(ItemCapability<?, ?> cap) {
        return CapabilityKeyIndex.getKey(cap);
    }
}
