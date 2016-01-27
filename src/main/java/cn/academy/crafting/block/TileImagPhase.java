package cn.academy.crafting.block;

import cn.academy.crafting.client.render.block.RenderImagPhaseLiquid;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.tileentity.TileEntity;

@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileImagPhase extends TileEntity {
    @RegTileEntity.Render
    @SideOnly(Side.CLIENT)
    public static RenderImagPhaseLiquid renderer;
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
}