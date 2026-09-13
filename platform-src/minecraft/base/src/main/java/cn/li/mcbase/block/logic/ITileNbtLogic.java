package cn.li.mcbase.block.logic;

import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import net.minecraft.nbt.CompoundTag;

public interface ITileNbtLogic {
    void readNbt(IScriptedBlockEntity be, CompoundTag tag);

    void writeNbt(IScriptedBlockEntity be, CompoundTag tag);
}
