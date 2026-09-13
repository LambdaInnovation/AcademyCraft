package cn.li.mcver;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Version seam for block-entity persistence.
 * Contracts are shaped for 26.2 ({@link Io} over {@code ValueInput}/{@code ValueOutput}).
 * On 1.21.1 those types do not exist yet, so {@link Io} wraps a {@link CompoundTag}
 * + {@link HolderLookup.Provider} pair instead.
 */
public final class BlockEntityIo {
    private BlockEntityIo() {
    }

    /** Opaque handle over a persistence payload (tag + registries on 1.21.1). */
    public sealed interface Io {
    }

    private record TagIo(CompoundTag tag, HolderLookup.Provider registries) implements Io {
    }

    public static Io ofValueInput(CompoundTag tag, HolderLookup.Provider registries) {
        return new TagIo(Objects.requireNonNull(tag, "tag"),
            Objects.requireNonNull(registries, "registries"));
    }

    public static Io ofValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
        return ofValueInput(tag, registries);
    }

    public static CompoundTag asTag(Io io) {
        if (io instanceof TagIo tagIo) {
            return tagIo.tag();
        }
        throw new IllegalStateException("BlockEntityIo.Io is not backed by a CompoundTag");
    }

    public static HolderLookup.Provider asRegistries(Io io) {
        if (io instanceof TagIo tagIo) {
            return tagIo.registries();
        }
        throw new IllegalStateException("BlockEntityIo.Io is not backed by registries");
    }

    public static void load(BlockEntity be, Io io) {
        be.loadWithComponents(asTag(io), asRegistries(io));
    }

    public static CompoundTag getUpdateTag(BlockEntity be, HolderLookup.Provider registries) {
        return be.getUpdateTag(registries);
    }

    /**
     * Callback used by versioned block-entity subclasses when writing NBT.
     */
    @FunctionalInterface
    public interface AdditionalWriter {
        void write(Io io);
    }
}
