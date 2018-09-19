package cn.academy.block.tileentity;

import cn.academy.client.render.block.RenderImagPhaseLiquid;
import cn.lambdalib2.registry.mc.RegTileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.tileentity.TileEntity;

@RegTileEntity
public class TileImagPhase extends TileEntity {

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
}