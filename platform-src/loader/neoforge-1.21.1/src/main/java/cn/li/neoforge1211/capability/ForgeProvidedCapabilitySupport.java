package cn.li.neoforge1211.capability;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * NeoForge ships built-in capabilities through {@link Capabilities}.
 * Those tokens already exist; mods attach providers and map tokens via {@link CapabilityRegistry},
 * but must not create duplicate sided tokens for the same interfaces.
 *
 * <p>Strategy: direct {@code Class} identity comparison against the NeoForge capability
 * interfaces, returning the corresponding {@link Capabilities} block/item token.</p>
 */
public final class ForgeProvidedCapabilitySupport {

    private ForgeProvidedCapabilitySupport() {
    }

    /**
     * Returns true when {@code capabilityType} is a NeoForge-provided capability interface.
     */
    public static boolean isForgeProvidedCapabilityType(Class<?> capabilityType) {
        return blockCapabilityForType(capabilityType) != null
            || itemCapabilityForType(capabilityType) != null;
    }

    /**
     * Resolve the NeoForge block capability token for a provided interface, if any.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static BlockCapability<?, Direction> blockCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == null) {
            return null;
        }
        if (capabilityType == IItemHandler.class) {
            return Capabilities.ItemHandler.BLOCK;
        }
        if (capabilityType == IFluidHandler.class) {
            return Capabilities.FluidHandler.BLOCK;
        }
        if (capabilityType == IEnergyStorage.class) {
            return Capabilities.EnergyStorage.BLOCK;
        }
        return null;
    }

    /**
     * Resolve the NeoForge item capability token for a provided interface, if any.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static ItemCapability<?, Void> itemCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == null) {
            return null;
        }
        if (capabilityType == IItemHandler.class) {
            return Capabilities.ItemHandler.ITEM;
        }
        if (capabilityType == IFluidHandlerItem.class) {
            return Capabilities.FluidHandler.ITEM;
        }
        if (capabilityType == IEnergyStorage.class) {
            return Capabilities.EnergyStorage.ITEM;
        }
        return null;
    }

    /**
     * @deprecated Prefer {@link #blockCapabilityForType(Class)}.
     */
    @Deprecated
    @Nullable
    public static BlockCapability<?, Direction> forgeProvidedCapabilityForType(Class<?> capabilityType) {
        return blockCapabilityForType(capabilityType);
    }
}
