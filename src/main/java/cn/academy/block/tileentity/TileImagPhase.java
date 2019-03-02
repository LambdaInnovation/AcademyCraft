package cn.academy.block.tileentity;

import cn.lambdalib2.registry.mc.RegTileEntity;
import net.minecraft.tileentity.TileEntity;

@RegTileEntity
public class TileImagPhase extends TileEntity {

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
}