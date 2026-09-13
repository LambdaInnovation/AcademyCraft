package cn.li.neoforgebase.capability;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared reverse-index for capability token → string key lookups.
 */
public final class CapabilityKeyIndex {
    private static final Map<BlockCapability<?, ?>, String> BLOCK_TO_KEY =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<ItemCapability<?, ?>, String> ITEM_TO_KEY =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private CapabilityKeyIndex() {
    }

    public static void putBlock(BlockCapability<?, ?> cap, String key) {
        BLOCK_TO_KEY.put(cap, key);
    }

    public static void putItem(ItemCapability<?, ?> cap, String key) {
        ITEM_TO_KEY.put(cap, key);
    }

    @Nullable
    public static String getKey(BlockCapability<?, ?> cap) {
        return BLOCK_TO_KEY.get(cap);
    }

    @Nullable
    public static String getKey(ItemCapability<?, ?> cap) {
        return ITEM_TO_KEY.get(cap);
    }

    public static List<String> keysFor(BlockCapability<?, ?> cap) {
        if (cap == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        synchronized (BLOCK_TO_KEY) {
            for (Map.Entry<BlockCapability<?, ?>, String> entry : BLOCK_TO_KEY.entrySet()) {
                if (entry.getKey() == cap) {
                    keys.add(entry.getValue());
                }
            }
        }
        return keys;
    }
}
