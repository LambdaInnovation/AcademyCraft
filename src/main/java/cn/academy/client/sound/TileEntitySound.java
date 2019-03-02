package cn.academy.client.sound;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;

/**
 * Plays sound on a certain TileEntity. Automatically stops when it is invalid.
 */
public class TileEntitySound extends PositionedSound {

    private final TileEntity te;

    public TileEntitySound(TileEntity _te, String name) {
        super(_te.getPos().getX() +.5, _te.getPos().getY() + .5, _te.getPos().getZ() + .5,
            name, SoundCategory.BLOCKS);
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