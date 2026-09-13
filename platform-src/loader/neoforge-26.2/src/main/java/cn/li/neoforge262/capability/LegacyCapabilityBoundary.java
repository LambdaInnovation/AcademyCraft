package cn.li.neoforge262.capability;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Compatibility boundary for third-party providers that still expose the
 * pre-transfer NeoForge storage interfaces.
 *
 * <p>All first-party 26.2 providers use {@code ResourceHandler} or
 * {@code EnergyHandler}. Keeping the deprecated types in this class prevents
 * them from leaking into registration and resolution code.</p>
 */
@SuppressWarnings("removal")
final class LegacyCapabilityBoundary {

    private LegacyCapabilityBoundary() {
    }

    @Nullable
    static BlockCapability<?, Direction> blockCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == IItemHandler.class) {
            return Capabilities.Item.BLOCK;
        }
        if (capabilityType == IFluidHandler.class) {
            return Capabilities.Fluid.BLOCK;
        }
        if (capabilityType == IEnergyStorage.class) {
            return Capabilities.Energy.BLOCK;
        }
        return null;
    }

    @Nullable
    static ItemCapability<?, ?> itemCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == IItemHandler.class) {
            return Capabilities.Item.ITEM;
        }
        if (capabilityType == IFluidHandler.class) {
            return Capabilities.Fluid.ITEM;
        }
        if (capabilityType == IEnergyStorage.class) {
            return Capabilities.Energy.ITEM;
        }
        return null;
    }

    @Nullable
    static Object adaptBlock(BlockCapability<?, Direction> capability, @Nullable Object handler) {
        if (handler == null) {
            return null;
        }
        if (capability == Capabilities.Item.BLOCK && handler instanceof IItemHandler itemHandler) {
            return ItemHandlerAsResourceHandler.wrap(itemHandler);
        }
        if (capability == Capabilities.Fluid.BLOCK && handler instanceof IFluidHandler fluidHandler) {
            return FluidHandlerAsResourceHandler.wrap(fluidHandler);
        }
        if (capability == Capabilities.Energy.BLOCK && handler instanceof IEnergyStorage energyStorage) {
            return EnergyStorageAsHandler.wrap(energyStorage);
        }
        return handler;
    }

    /**
     * Same legacy→transfer wrap as {@link #adaptBlock}, for item-stack providers
     * that still expose pre-transfer storage interfaces.
     */
    @Nullable
    static Object adaptItem(ItemCapability<?, ?> capability, @Nullable Object handler) {
        if (handler == null) {
            return null;
        }
        if (capability == Capabilities.Item.ITEM && handler instanceof IItemHandler itemHandler) {
            return ItemHandlerAsResourceHandler.wrap(itemHandler);
        }
        if (capability == Capabilities.Fluid.ITEM && handler instanceof IFluidHandler fluidHandler) {
            return FluidHandlerAsResourceHandler.wrap(fluidHandler);
        }
        if (capability == Capabilities.Energy.ITEM && handler instanceof IEnergyStorage energyStorage) {
            return EnergyStorageAsHandler.wrap(energyStorage);
        }
        return handler;
    }
}
