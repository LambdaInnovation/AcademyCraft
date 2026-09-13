package cn.li.mcbase.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface ScriptedServerTickDispatcher {
    void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity);
}
