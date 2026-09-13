package cn.li.mcver;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Version seam for block-entity persistence.
 * Contracts are shaped for 26.2 ({@link Io} over {@code ValueInput}/{@code ValueOutput}).
 * On 1.20.1 neither those types nor {@code HolderLookup.Provider} exist, so {@link Io}
 * wraps a bare {@link CompoundTag}.
 */
public final class BlockEntityIo {
    private BlockEntityIo() {
    }

    /** Opaque handle over a persistence payload (CompoundTag on 1.20.1). */
    public sealed interface Io {
    }

    private record TagIo(CompoundTag tag) implements Io {
    }

    public static Io ofValueInput(CompoundTag tag) {
        return new TagIo(Objects.requireNonNull(tag, "tag"));
    }

    public static Io ofValueOutput(CompoundTag tag) {
        return ofValueInput(tag);
    }

    public static CompoundTag asTag(Io io) {
        if (io instanceof TagIo tagIo) {
            return tagIo.tag();
        }
        throw new IllegalStateException("BlockEntityIo.Io is not backed by a CompoundTag");
    }

    public static void load(BlockEntity be, Io io) {
        be.load(asTag(io));
    }

    public static CompoundTag getUpdateTag(BlockEntity be) {
        return be.getUpdateTag();
    }

    /**
     * Callback used by versioned block-entity subclasses when writing NBT.
     */
    @FunctionalInterface
    public interface AdditionalWriter {
        void write(Io io);
    }
}
