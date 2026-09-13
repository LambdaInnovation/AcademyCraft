package cn.li.mc1211.integration.saveddata;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;

public class WorldLifecycleSavedData extends SavedData {
    public static final String NAME = "clj_world_lifecycle";

    private CompoundTag handlers;

    public WorldLifecycleSavedData() {
        this.handlers = new CompoundTag();
    }

    public WorldLifecycleSavedData(CompoundTag handlers) {
        this.handlers = handlers == null ? new CompoundTag() : handlers;
    }

    public static WorldLifecycleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag handlers = tag.getCompound("handlers");
        return new WorldLifecycleSavedData(handlers.copy());
    }

    public static Factory<WorldLifecycleSavedData> factory() {
        return new Factory<>(WorldLifecycleSavedData::new, WorldLifecycleSavedData::load,
            DataFixTypes.LEVEL);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("handlers", handlers.copy());
        return tag;
    }

    public CompoundTag getHandlers() {
        return handlers;
    }

    public void setHandlers(CompoundTag handlers) {
        this.handlers = handlers == null ? new CompoundTag() : handlers;
        setDirty();
    }
}
