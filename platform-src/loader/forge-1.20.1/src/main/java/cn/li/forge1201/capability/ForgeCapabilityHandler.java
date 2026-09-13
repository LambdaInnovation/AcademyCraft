package cn.li.forge1201.capability;

import cn.li.mcbase.block.capability.ScriptedCapabilityResolver;
import cn.li.mc1201.block.entity.AbstractScriptedBlockEntity;
import cn.li.forge1201.block.entity.ScriptedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Forge capability resolution and invalidation for scripted block entities.
 *
 * <p>Orchestration mirrors NeoForge: static {@link #resolve} + {@link #invalidateAt}.
 * {@link LazyOptional} exists only at the {@code ICapabilityProvider#getCapability}
 * return boundary (Forge API requirement). A thin per-BE live map tracks returned
 * optionals so {@link #invalidateAt} / {@code invalidateCaps} can invalidate them.</p>
 */
public final class ForgeCapabilityHandler {

    private ForgeCapabilityHandler() {
    }

    /**
     * Resolve a scripted capability handler, or {@code null} if unsupported.
     */
    @Nullable
    public static Object resolve(@Nullable AbstractScriptedBlockEntity be,
                                 @Nonnull Capability<?> cap,
                                 @Nullable Direction side) {
        if (be == null) {
            return null;
        }
        String key = ForgeCapabilityQuery.getKey(cap);
        if (key == null) {
            return null;
        }
        return ScriptedCapabilityResolver.resolve(be, key, side);
    }

    /**
     * Wrap a resolved handler as {@link LazyOptional} at the Forge provider boundary,
     * tracking it on {@code live} for later invalidation.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> wrapBoundary(@Nonnull Map<String, LazyOptional<?>> live,
                                                   @Nonnull Capability<T> cap,
                                                   @Nonnull Object handler) {
        String key = ForgeCapabilityQuery.getKey(cap);
        if (key == null) {
            return LazyOptional.empty();
        }
        LazyOptional<?> existing = live.get(key);
        if (existing != null && existing.isPresent()) {
            return existing.cast();
        }
        LazyOptional<Object> optional = LazyOptional.of(() -> handler);
        live.put(key, optional);
        return optional.cast();
    }

    /**
     * Invalidate live boundary optionals tracked for one block entity.
     */
    public static void invalidateLive(@Nonnull Map<String, LazyOptional<?>> live) {
        live.values().forEach(LazyOptional::invalidate);
        live.clear();
    }

    /**
     * Invalidate Forge capability optionals for the scripted block entity at {@code pos}.
     */
    public static void invalidateAt(@Nullable Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ScriptedBlockEntity scripted) {
            scripted.invalidateCaps();
        }
    }
}
