package cn.academy.block.tileentity;

import cn.academy.client.render.block.RenderImagPhaseLiquid;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.tileentity.TileEntity;

@RegTileEntity
public class TileImagPhase extends TileEntity {
    @RegTileEntity.Render
    @SideOnly(Side.CLIENT)
    public static RenderImagPhaseLiquid renderer;
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
}