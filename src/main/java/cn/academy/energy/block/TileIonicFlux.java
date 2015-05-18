package cn.academy.energy.block;

import net.minecraft.tileentity.TileEntity;
import cn.academy.energy.client.render.block.RenderIonicFlux;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileIonicFlux extends TileEntity {
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderIonicFlux renderer;
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
}