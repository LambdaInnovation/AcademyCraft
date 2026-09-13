package cn.li.mcbase.shim;

import cn.li.mcbase.block.logic.ITileTickLogic;
import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import clojure.lang.IFn;

public class FnTileTickLogic implements ITileTickLogic {
    private final IFn fn;
    public FnTileTickLogic(IFn fn) { this.fn = fn; }
    @Override public void serverTick(Level level, BlockPos pos, BlockState state, IScriptedBlockEntity be) {
        fn.invoke(level, pos, state, be);
    }
}
