package cn.li.mcver;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Version seam for block-entity persistence.
 * 26.2 replaced the {@code CompoundTag + HolderLookup.Provider}
 * {@code loadAdditional}/{@code saveAdditional} signatures with a single
 * {@link ValueInput}/{@link ValueOutput} parameter. {@link Io} is an opaque
 * wrapper so callers written against either direction share one type;
 * unwrap with {@link #asValueInput} / {@link #asValueOutput} only from
 * version-specific code that already knows which direction it holds.
 *
 * <p>{@code getUpdateTag} was not touched by the ValueInput/ValueOutput
 * migration -- {@link BlockEntity#getUpdateTag(HolderLookup.Provider)} still
 * takes the registries provider directly and returns a {@link CompoundTag}.
 */
public final class BlockEntityIo {
    private BlockEntityIo() {
    }

    /** Opaque handle over either a {@link ValueInput} or a {@link ValueOutput}. */
    public sealed interface Io {
    }

    private record InputIo(ValueInput value) implements Io {
    }

    private record OutputIo(ValueOutput value) implements Io {
    }

    public static Io ofValueInput(ValueInput in) {
        return new InputIo(Objects.requireNonNull(in, "in"));
    }

    public static Io ofValueOutput(ValueOutput out) {
        return new OutputIo(Objects.requireNonNull(out, "out"));
    }

    public static ValueInput asValueInput(Io io) {
        if (io instanceof InputIo input) {
            return input.value();
        }
        throw new IllegalStateException("BlockEntityIo.Io is not backed by a ValueInput");
    }

    public static ValueOutput asValueOutput(Io io) {
        if (io instanceof OutputIo output) {
            return output.value();
        }
        throw new IllegalStateException("BlockEntityIo.Io is not backed by a ValueOutput");
    }

    public static void load(BlockEntity be, Io io) {
        be.loadWithComponents(asValueInput(io));
    }

    public static CompoundTag getUpdateTag(BlockEntity be, HolderLookup.Provider registries) {
        return be.getUpdateTag(registries);
    }

    /**
     * Callback used by versioned block-entity subclasses when writing NBT
     * from their {@code saveAdditional(ValueOutput)} override.
     */
    @FunctionalInterface
    public interface AdditionalWriter {
        void write(Io io);
    }
}
