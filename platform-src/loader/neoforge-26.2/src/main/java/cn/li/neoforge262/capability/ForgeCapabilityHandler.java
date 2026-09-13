package cn.li.neoforge262.capability;

import cn.li.neoforgebase.capability.ForgeCapabilityQuery;
import cn.li.mcbase.block.capability.ScriptedCapabilityResolver;
import cn.li.mc262.block.entity.AbstractScriptedBlockEntity;
import cn.li.mc262.block.entity.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;

/**
 * NeoForge capability registration and resolution for scripted block entities.
 *
 * <p>Providers are registered on {@link RegisterCapabilitiesEvent}; queries return a nullable
 * handler. Call {@link #invalidateAt(Level, BlockPos)} when a block capability
 * appears, disappears, or must be re-resolved.</p>
 */
public final class ForgeCapabilityHandler {

    private ForgeCapabilityHandler() {
    }

    /**
     * Register providers for every known block capability on every scripted block-entity type.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerAll(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<?> type : BlockEntityRegistry.allTypes()) {
            for (BlockCapability<?, Direction> cap : CapabilityRegistry.allBlockCapabilities()) {
                BlockCapability blockCap = cap;
                BlockEntityType beType = type;
                event.registerBlockEntity(
                    blockCap,
                    beType,
                    (be, side) -> resolve(be, blockCap, (Direction) side));
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T resolve(@Nullable BlockEntity be,
                                BlockCapability<T, Direction> cap,
                                @Nullable Direction side) {
        if (!(be instanceof AbstractScriptedBlockEntity scripted)) {
            return null;
        }
        for (String key : CapabilityRegistry.keysFor(cap)) {
            Object handler = LegacyCapabilityBoundary.adaptBlock(
                cap,
                ScriptedCapabilityResolver.resolve(scripted, key, side));
            if (handler != null) {
                return (T) handler;
            }
        }
        String key = ForgeCapabilityQuery.getKey(cap);
        if (key == null) {
            return null;
        }
        Object handler = LegacyCapabilityBoundary.adaptBlock(
            cap,
            ScriptedCapabilityResolver.resolve(scripted, key, side));
        if (handler == null) {
            return null;
        }
        return (T) handler;
    }

    /**
     * Notify NeoForge capability caches that providers at {@code pos} may have changed.
     */
    public static void invalidateAt(@Nullable Level level, BlockPos pos) {
        if (level != null && pos != null) {
            level.invalidateCapabilities(pos);
        }
    }
}
