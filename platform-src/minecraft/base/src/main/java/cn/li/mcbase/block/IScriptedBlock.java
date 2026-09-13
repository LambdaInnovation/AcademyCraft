package cn.li.mcbase.block;

import cn.li.mcbase.block.logic.TileLogicBundle;

public interface IScriptedBlock {
    TileLogicBundle getTileLogic();

    String getTileId();

    String getBlockId();

    void installTileLogic(TileLogicBundle bundle);
}
