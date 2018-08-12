package cn.academy.client.sound;

import net.minecraft.tileentity.TileEntity;

/**
 * Plays sound on a certain TileEntity. Automatically stops when it is invalid.
 */
public class TileEntitySound extends PositionedSound {

    private final TileEntity te;

    public TileEntitySound(TileEntity _te, String name) {
        super(_te.x +.5, _te.y + .5, _te.z + .5, name);
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