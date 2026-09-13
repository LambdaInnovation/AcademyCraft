package cn.li.mcver;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Cross-version CompoundTag/ListTag accessors.
 * 26.2 contract: untyped contains + OrEmpty/Or-default reads.
 */
public final class NbtAccess {
    private NbtAccess() {
    }

    public static boolean contains(CompoundTag tag, String key) {
        return tag != null && tag.contains(key);
    }

    public static byte getByte(CompoundTag tag, String key) {
        return tag.getByte(key);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBoolean(key);
    }

    public static long getLong(CompoundTag tag, String key) {
        return tag.getLong(key);
    }

    public static double getDouble(CompoundTag tag, String key) {
        return tag.getDouble(key);
    }

    public static float getFloat(CompoundTag tag, String key) {
        return tag.getFloat(key);
    }

    public static int getInt(CompoundTag tag, String key) {
        return tag.getInt(key);
    }

    public static String getString(CompoundTag tag, String key) {
        return tag.getString(key);
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return tag.getCompound(key);
    }

    @Nullable
    public static CompoundTag getCompoundOrNull(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(key);
    }

    public static ListTag getList(CompoundTag tag, String key) {
        return tag.getList(key, Tag.TAG_COMPOUND);
    }

    public static CompoundTag getCompoundAt(ListTag list, int index) {
        return list.getCompound(index);
    }

    public static Set<String> keySet(CompoundTag tag) {
        return tag.getAllKeys();
    }

    public static void put(CompoundTag tag, String key, Tag value) {
        tag.put(key, value);
    }
}
