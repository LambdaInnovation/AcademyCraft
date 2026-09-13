package cn.li.mc262.bridge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * @deprecated Use {@link cn.li.mcver.NbtAccess}.
 */
@Deprecated
public final class NbtAccess {
    private NbtAccess() {
    }

    public static boolean contains(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.contains(tag, key);
    }

    public static byte getByte(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getByte(tag, key);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getBoolean(tag, key);
    }

    public static long getLong(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getLong(tag, key);
    }

    public static double getDouble(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getDouble(tag, key);
    }

    public static float getFloat(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getFloat(tag, key);
    }

    public static int getInt(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getInt(tag, key);
    }

    public static String getString(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getString(tag, key);
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getCompound(tag, key);
    }

    @Nullable
    public static CompoundTag getCompoundOrNull(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getCompoundOrNull(tag, key);
    }

    public static ListTag getList(CompoundTag tag, String key) {
        return cn.li.mcver.NbtAccess.getList(tag, key);
    }

    public static CompoundTag getCompoundAt(ListTag list, int index) {
        return cn.li.mcver.NbtAccess.getCompoundAt(list, index);
    }

    public static Set<String> keySet(CompoundTag tag) {
        return cn.li.mcver.NbtAccess.keySet(tag);
    }

    public static void put(CompoundTag tag, String key, Tag value) {
        cn.li.mcver.NbtAccess.put(tag, key, value);
    }
}
