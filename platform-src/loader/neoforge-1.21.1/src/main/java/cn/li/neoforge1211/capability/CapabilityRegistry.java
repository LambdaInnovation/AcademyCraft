package cn.li.neoforge1211.capability;

import cn.li.neoforgebase.capability.CapabilityKeyIndex;

import cn.li.mcmod.ModId;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Named capability registry for NeoForge {@link BlockCapability} / {@link ItemCapability} tokens.
 *
 * <p>Stable string keys (e.g. {@code "fluid-handler"}, {@code "forge-energy"}) map to NeoForge
 * capability tokens. Built-in NeoForge capabilities should be registered explicitly; unknown
 * keys are lazily backed by {@link BlockCapability#createSided}.</p>
 */
public final class CapabilityRegistry {

    private static final Map<String, BlockCapability<?, Direction>> KEY_TO_BLOCK = new ConcurrentHashMap<>();
    private static final Map<BlockCapability<?, ?>, String> BLOCK_TO_KEY = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<String, ItemCapability<?, Void>> KEY_TO_ITEM = new ConcurrentHashMap<>();
    private static final Map<ItemCapability<?, ?>, String> ITEM_TO_KEY = Collections.synchronizedMap(new IdentityHashMap<>());

    private CapabilityRegistry() {
    }

    public static synchronized <T> void registerBlock(String key, BlockCapability<T, Direction> cap) {
        KEY_TO_BLOCK.put(key, cap);
        BLOCK_TO_KEY.put(cap, key);
        CapabilityKeyIndex.putBlock(cap, key);
    }

    public static synchronized <T> void registerItem(String key, ItemCapability<T, Void> cap) {
        KEY_TO_ITEM.put(key, cap);
        ITEM_TO_KEY.put(cap, key);
        CapabilityKeyIndex.putItem(cap, key);
    }

    /** Maps a string key to a block capability token (NeoForge replacement for Forge Capability tokens). */
    public static synchronized <T> void register(String key, BlockCapability<T, Direction> cap) {
        registerBlock(key, cap);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> BlockCapability<T, Direction> getOrCreateBlock(String key, Class<T> type) {
        BlockCapability<?, Direction> existing = KEY_TO_BLOCK.get(key);
        if (existing != null) {
            return (BlockCapability<T, Direction>) existing;
        }
        BlockCapability<T, Direction> cap = BlockCapability.createSided(capabilityId(key), type);
        registerBlock(key, cap);
        return cap;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> BlockCapability<T, Direction> getBlock(String key) {
        return (BlockCapability<T, Direction>) KEY_TO_BLOCK.get(key);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> ItemCapability<T, Void> getItem(String key) {
        return (ItemCapability<T, Void>) KEY_TO_ITEM.get(key);
    }

    @Nullable
    public static String getKey(BlockCapability<?, ?> cap) {
        return CapabilityKeyIndex.getKey(cap);
    }

    @Nullable
    public static String getKey(ItemCapability<?, ?> cap) {
        return CapabilityKeyIndex.getKey(cap);
    }

    public static Collection<BlockCapability<?, Direction>> allBlockCapabilities() {
        return Collections.unmodifiableCollection(KEY_TO_BLOCK.values());
    }

    public static Collection<ItemCapability<?, Void>> allItemCapabilities() {
        return Collections.unmodifiableCollection(KEY_TO_ITEM.values());
    }

    private static ResourceLocation capabilityId(String key) {
        String path = key.replace(':', '_').replace(' ', '_').toLowerCase();
        return ResourceLocation.fromNamespaceAndPath(ModId.ID, "cap/" + path);
    }
}
