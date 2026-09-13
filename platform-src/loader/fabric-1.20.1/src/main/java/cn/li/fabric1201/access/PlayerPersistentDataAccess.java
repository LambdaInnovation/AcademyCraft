package cn.li.fabric1201.access;

import net.minecraft.nbt.CompoundTag;

/**
 * Duck interface for player persistent data. Kept outside the mixin package so
 * runtime code can cast to it without IllegalClassLoadError.
 */
public interface PlayerPersistentDataAccess {
    CompoundTag academy$getPersistentData();
}
