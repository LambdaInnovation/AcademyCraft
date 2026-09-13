package cn.li.mcbase.block.capability;

import cn.li.mcbase.block.IScriptedBlock;
import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import cn.li.mcbase.block.logic.ITileCapabilityLogic;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Resolves capability handlers from compiled tile logic bundles on scripted blocks.
 */
public final class ScriptedCapabilityResolver {

    private ScriptedCapabilityResolver() {
    }

    @Nullable
    public static Object resolve(BlockEntity be, String key, @Nullable Direction side) {
        if (!(be instanceof IScriptedBlockEntity scriptedBe) || key == null) {
            return null;
        }
        Block block = be.getBlockState().getBlock();
        if (!(block instanceof IScriptedBlock scripted)) {
            return null;
        }
        ITileCapabilityLogic capability = scripted.getTileLogic().capability;
        if (capability == null) {
            return null;
        }
        return capability.resolve(scriptedBe, key, side);
    }
}
