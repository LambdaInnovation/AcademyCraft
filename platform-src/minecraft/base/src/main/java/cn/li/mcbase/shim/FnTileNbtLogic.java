package cn.li.mcbase.shim;

import cn.li.mcbase.block.logic.ITileNbtLogic;
import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import net.minecraft.nbt.CompoundTag;
import clojure.lang.IFn;

public class FnTileNbtLogic implements ITileNbtLogic {
    private final IFn readFn;
    private final IFn writeFn;
    public FnTileNbtLogic(IFn readFn, IFn writeFn) { this.readFn = readFn; this.writeFn = writeFn; }
    @Override public void readNbt(IScriptedBlockEntity be, CompoundTag tag) {
        if (readFn != null) readFn.invoke(be, tag);
    }
    @Override public void writeNbt(IScriptedBlockEntity be, CompoundTag tag) {
        if (writeFn != null) writeFn.invoke(be, tag);
    }
}
