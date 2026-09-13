package cn.li.forge1201.block.entity;

import cn.li.forge1201.capability.ForgeCapabilityHandler;
import cn.li.mcbase.block.IScriptedBlock;
import cn.li.mc1201.block.entity.AbstractScriptedBlockEntity;
import cn.li.mc1201.block.entity.BlockEntityRegistry;
import cn.li.mcbase.block.logic.ITileContainerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge scripted block entity with capability and container support.
 *
 * <p>Capabilities resolve via {@link ForgeCapabilityHandler#resolve}; {@link LazyOptional}
 * is created only at {@link #getCapability}. Call {@link ForgeCapabilityHandler#invalidateAt}
 * (or {@link #invalidateCaps}) when providers change.</p>
 *
 * <p>Core scripted state/NBT/tick behavior is implemented in
 * {@link AbstractScriptedBlockEntity}.</p>
 */
public class ScriptedBlockEntity extends AbstractScriptedBlockEntity implements WorldlyContainer {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * Register this entity type via the shared registry.
     */
    public static void registerType(String tileId, BlockEntityType<ScriptedBlockEntity> type) {
        BlockEntityRegistry.registerType(tileId, type);
    }

    /**
     * Retrieve a registered entity type via the shared registry.
     */
    @Nullable
    public static BlockEntityType<ScriptedBlockEntity> getType(String tileId) {
        return (BlockEntityType<ScriptedBlockEntity>) BlockEntityRegistry.getType(tileId);
    }

    /** Thin live map of boundary LazyOptionals for this BE (invalidated on invalidateCaps). */
    private final Map<String, LazyOptional<?>> liveCapabilityOptionals = new ConcurrentHashMap<>();

    public ScriptedBlockEntity(BlockEntityType<ScriptedBlockEntity> type,
                               BlockPos pos,
                               BlockState state,
                               String tileId,
                               String blockId) {
        super(type, pos, state, tileId, blockId);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ScriptedBlockEntity blockEntity) {
        invokeServerTick(level, pos, state, blockEntity);
    }

    /**
     * Invalidate Forge capability caches/optionals for this entity's position.
     */
    public void invalidateCapabilities() {
        ForgeCapabilityHandler.invalidateAt(getLevel(), getBlockPos());
    }

    @Nullable
    private ITileContainerLogic containerLogic() {
        Block block = getBlockState().getBlock();
        if (block instanceof IScriptedBlock scripted) {
            return scripted.getTileLogic().container;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Forge Capability
    // -------------------------------------------------------------------------

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        Object handler = ForgeCapabilityHandler.resolve(this, cap, side);
        if (handler != null) {
            return ForgeCapabilityHandler.wrapBoundary(liveCapabilityOptionals, cap, handler);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        ForgeCapabilityHandler.invalidateLive(liveCapabilityOptionals);
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        // No eager work — capabilities are recreated lazily on next getCapability.
    }

    // -------------------------------------------------------------------------
    // Container / WorldlyContainer — dispatches through compiled ITileContainerLogic.
    // -------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        ITileContainerLogic container = containerLogic();
        return container == null ? 0 : container.getSize(this);
    }

    @Override
    public boolean isEmpty() {
        int size = getContainerSize();
        for (int i = 0; i < size; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getItem(int slot) {
        ITileContainerLogic container = containerLogic();
        if (container == null) return ItemStack.EMPTY;
        ItemStack result = container.getItem(this, slot);
        return result != null ? result : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ITileContainerLogic container = containerLogic();
        if (container == null) return ItemStack.EMPTY;
        ItemStack result = container.removeItem(this, slot, amount);
        return result != null ? result : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ITileContainerLogic container = containerLogic();
        if (container == null) return ItemStack.EMPTY;
        ItemStack result = container.removeItemNoUpdate(this, slot);
        return result != null ? result : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, @Nonnull ItemStack stack) {
        ITileContainerLogic container = containerLogic();
        if (container != null) {
            container.setItem(this, slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        ITileContainerLogic container = containerLogic();
        return container == null || container.stillValid(this, player);
    }

    @Override
    public void clearContent() {
        ITileContainerLogic container = containerLogic();
        if (container != null) {
            container.clearContent(this);
            setChanged();
        }
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull Direction side) {
        ITileContainerLogic container = containerLogic();
        if (container == null) {
            return EMPTY_INT_ARRAY;
        }
        return container.getSlotsForFace(this, side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @Nonnull ItemStack item, @Nullable Direction dir) {
        ITileContainerLogic container = containerLogic();
        return container != null && container.canPlaceItemThroughFace(this, slot, item, dir);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @Nonnull ItemStack item, @Nonnull Direction dir) {
        ITileContainerLogic container = containerLogic();
        return container == null || container.canTakeItemThroughFace(this, slot, item, dir);
    }
}
