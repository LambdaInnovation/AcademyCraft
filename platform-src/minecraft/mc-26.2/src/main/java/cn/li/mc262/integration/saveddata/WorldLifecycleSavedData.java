package cn.li.mc262.integration.saveddata;

import cn.li.mcmod.ModId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Dimension-scoped world-lifecycle payload store (handlers CompoundTag map).
 *
 * <p>26.2 uses {@link SavedDataType} + {@link Codec} instead of
 * {@code SavedData.Factory} / {@code DimensionDataStorage}.</p>
 */
public class WorldLifecycleSavedData extends SavedData {
    public static final String NAME = "clj_world_lifecycle";
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ModId.ID, NAME);

    public static final Codec<WorldLifecycleSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            CompoundTag.CODEC.fieldOf("handlers").forGetter(WorldLifecycleSavedData::getHandlers)
        ).apply(instance, WorldLifecycleSavedData::new));

    public static final SavedDataType<WorldLifecycleSavedData> TYPE =
        new SavedDataType<>(ID, WorldLifecycleSavedData::new, CODEC, null);

    private CompoundTag handlers;

    public WorldLifecycleSavedData() {
        this(new CompoundTag());
    }

    public WorldLifecycleSavedData(CompoundTag handlers) {
        this.handlers = handlers == null ? new CompoundTag() : handlers;
    }

    public CompoundTag getHandlers() {
        return handlers;
    }

    public void setHandlers(CompoundTag handlers) {
        this.handlers = handlers == null ? new CompoundTag() : handlers;
        setDirty();
    }
}
