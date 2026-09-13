package cn.li.neoforge262.capability;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

/**
 * NeoForge 26.2 built-in capabilities use the transfer API
 * ({@link EnergyHandler}, {@link ResourceHandler}).
 *
 * <p>{@link EnergyHandler} maps directly onto the energy tokens.
 * {@link ResourceHandler} is shared by item and fluid, so callers use the
 * explicit family accessors when the resource kind matters. Prefer
 * {@link #energyBlock()}, {@link #fluidBlock()}, {@link #itemBlock()}, and
 * their ITEM counterparts when registering transfer-facing providers.</p>
 *
 * <p>Deprecated storage interfaces are recognized only by
 * {@link LegacyCapabilityBoundary}, the third-party compatibility edge.</p>
 */
public final class ForgeProvidedCapabilitySupport {

    private ForgeProvidedCapabilitySupport() {
    }

    public static BlockCapability<EnergyHandler, Direction> energyBlock() {
        return Capabilities.Energy.BLOCK;
    }

    public static BlockCapability<ResourceHandler<FluidResource>, Direction> fluidBlock() {
        return Capabilities.Fluid.BLOCK;
    }

    public static BlockCapability<ResourceHandler<ItemResource>, Direction> itemBlock() {
        return Capabilities.Item.BLOCK;
    }

    public static ItemCapability<ResourceHandler<ItemResource>, ?> itemItem() {
        return Capabilities.Item.ITEM;
    }

    public static ItemCapability<ResourceHandler<FluidResource>, ?> fluidItem() {
        return Capabilities.Fluid.ITEM;
    }

    public static ItemCapability<EnergyHandler, ?> energyItem() {
        return Capabilities.Energy.ITEM;
    }

    public static boolean isForgeProvidedCapabilityType(Class<?> capabilityType) {
        return blockCapabilityForType(capabilityType) != null
            || itemCapabilityForType(capabilityType) != null;
    }

    @Nullable
    public static BlockCapability<?, Direction> blockCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == null) {
            return null;
        }
        if (capabilityType == EnergyHandler.class) {
            return Capabilities.Energy.BLOCK;
        }
        // ResourceHandler is shared by item+fluid; callers must use fluidBlock()/itemBlock().
        return LegacyCapabilityBoundary.blockCapabilityForType(capabilityType);
    }

    @Nullable
    public static ItemCapability<?, ?> itemCapabilityForType(Class<?> capabilityType) {
        if (capabilityType == null) {
            return null;
        }
        if (capabilityType == EnergyHandler.class) {
            return Capabilities.Energy.ITEM;
        }
        if (capabilityType == ResourceHandler.class) {
            // Preserve item as the default; fluid callers must use fluidItem().
            return Capabilities.Item.ITEM;
        }
        return LegacyCapabilityBoundary.itemCapabilityForType(capabilityType);
    }

    @Deprecated
    @Nullable
    public static BlockCapability<?, Direction> forgeProvidedCapabilityForType(Class<?> capabilityType) {
        return blockCapabilityForType(capabilityType);
    }
}
