package cn.academy.core.client.sound;

import net.minecraft.tileentity.TileEntity;

/**
 * Plays sound on a certain TileEntity. Automatically stops when it is invalid.
 */
public class TileEntitySound extends PositionedSound {

    private final TileEntity te;

    public TileEntitySound(TileEntity _te, String name) {
        super(_te.xCoord +.5, _te.yCoord + .5, _te.zCoord + .5, name);
        te = _te;
    }

    @Override
    public void update() {
        super.update();

        if (te.isInvalid()) {
            donePlaying = true;
        }
    }
}
