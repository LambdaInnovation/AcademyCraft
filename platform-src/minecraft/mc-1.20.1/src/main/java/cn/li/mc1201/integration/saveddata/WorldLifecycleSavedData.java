package cn.li.mc1201.integration.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class WorldLifecycleSavedData extends SavedData {
    public static final String NAME = "clj_world_lifecycle";

    private CompoundTag handlers;

    public WorldLifecycleSavedData() {
        this.handlers = new CompoundTag();
    }

    public WorldLifecycleSavedData(CompoundTag handlers) {
        this.handlers = handlers == null ? new CompoundTag() : handlers;
    }

    public static WorldLifecycleSavedData load(CompoundTag tag) {
        CompoundTag handlers = tag.getCompound("handlers");
        return new WorldLifecycleSavedData(handlers.copy());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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
