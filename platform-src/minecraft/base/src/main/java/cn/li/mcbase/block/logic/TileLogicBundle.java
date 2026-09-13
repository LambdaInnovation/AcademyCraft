package cn.li.mcbase.block.logic;

import javax.annotation.Nullable;

public final class TileLogicBundle {
    public static final TileLogicBundle EMPTY = new TileLogicBundle(null, null, null, null);

    public final @Nullable ITileTickLogic tick;
    public final @Nullable ITileNbtLogic nbt;
    public final @Nullable ITileContainerLogic container;
    public final @Nullable ITileCapabilityLogic capability;

    public TileLogicBundle(@Nullable ITileTickLogic tick,
                           @Nullable ITileNbtLogic nbt,
                           @Nullable ITileContainerLogic container,
                           @Nullable ITileCapabilityLogic capability) {
        this.tick = tick;
        this.nbt = nbt;
        this.container = container;
        this.capability = capability;
    }
}
