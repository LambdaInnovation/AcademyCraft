package cn.li.mcbase.block.logic;

import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface ITileTickLogic {
    void serverTick(Level level, BlockPos pos, BlockState state, IScriptedBlockEntity be);
}
