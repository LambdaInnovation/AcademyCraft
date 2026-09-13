package cn.li.forge1201.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

/**
 * Forge ships built-in capabilities through {@link ForgeCapabilities} tokens.
 * Those interfaces are already registered before mod {@code RegisterCapabilitiesEvent}
 * handlers run; mods attach handlers and map tokens via {@link CapabilityRegistry},
 * but must not call {@code RegisterCapabilitiesEvent#register} for them again.
 *
 * <p>Strategy: direct {@code Class} identity comparison against the Forge capability
 * interfaces, returning the corresponding {@link ForgeCapabilities} token. Zero string
 * comparison, zero ASM dependency, zero {@code isRegistered()} timing issue.</p>
 */
public final class ForgeProvidedCapabilitySupport {

    private ForgeProvidedCapabilitySupport() {
    }

    /**
     * Returns true when {@code capabilityType} is a Forge-provided capability interface
     * already registered through {@link ForgeCapabilities}.
     */
    public static boolean isForgeProvidedCapabilityType(Class<?> capabilityType) {
        return forgeProvidedCapabilityForType(capabilityType) != null;
    }

    /**
     * Resolve the {@link ForgeCapabilities} token for a Forge-provided interface, if any.
     * Uses Class identity comparison ({@code ==}) — the fastest, most readable approach.
     */
    public static Capability<?> forgeProvidedCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == null) {
            return null;
        }
        if (capabilityType == IItemHandler.class) {
            return ForgeCapabilities.ITEM_HANDLER;
        }
        if (capabilityType == IFluidHandler.class) {
            return ForgeCapabilities.FLUID_HANDLER;
        }
        if (capabilityType == IFluidHandlerItem.class) {
            return ForgeCapabilities.FLUID_HANDLER_ITEM;
        }
        if (capabilityType == IEnergyStorage.class) {
            return ForgeCapabilities.ENERGY;
        }
        return null;
    }
}
