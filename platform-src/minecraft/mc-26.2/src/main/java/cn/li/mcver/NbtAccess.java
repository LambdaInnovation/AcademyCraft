package cn.li.mcver;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Cross-version CompoundTag/ListTag accessors.
 * 26.2: Optional / OrEmpty / Or-default getters; untyped contains.
 */
public final class NbtAccess {
    private NbtAccess() {
    }

    public static boolean contains(CompoundTag tag, String key) {
        return tag != null && tag.contains(key);
    }

    public static byte getByte(CompoundTag tag, String key) {
        return tag.getByteOr(key, (byte) 0);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBooleanOr(key, false);
    }

    public static long getLong(CompoundTag tag, String key) {
        return tag.getLongOr(key, 0L);
    }

    public static double getDouble(CompoundTag tag, String key) {
        return tag.getDoubleOr(key, 0.0d);
    }

    public static float getFloat(CompoundTag tag, String key) {
        return tag.getFloatOr(key, 0.0f);
    }

    public static int getInt(CompoundTag tag, String key) {
        return tag.getIntOr(key, 0);
    }

    public static String getString(CompoundTag tag, String key) {
        return tag.getStringOr(key, "");
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return tag.getCompoundOrEmpty(key);
    }

    @Nullable
    public static CompoundTag getCompoundOrNull(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key)) {
            return null;
        }
        return tag.getCompoundOrEmpty(key);
    }

    public static ListTag getList(CompoundTag tag, String key) {
        return tag.getListOrEmpty(key);
    }

    public static CompoundTag getCompoundAt(ListTag list, int index) {
        return list.getCompoundOrEmpty(index);
    }

    public static Set<String> keySet(CompoundTag tag) {
        return tag.keySet();
    }

    public static void put(CompoundTag tag, String key, Tag value) {
        tag.put(key, value);
    }
}
