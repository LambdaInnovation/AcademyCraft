package cn.li.mcbase.block.logic;

import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import javax.annotation.Nullable;

public interface ITileCapabilityLogic {
    @Nullable
    Object resolve(IScriptedBlockEntity be, String capKey, @Nullable Object side);
}
