/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACModels;
import cn.liutils.api.render.model.TileEntityModelCustom;
import cn.liutils.template.client.render.block.RenderDirMultiModelled;

/**
 * @author WeathFolD
 *
 */
public class RenderMagInducer extends RenderDirMultiModelled {
	
	private static ResourceLocation TEX = new ResourceLocation("academy:textures/models/magincr.png");

	public RenderMagInducer() {
		super(new TileEntityModelCustom(ACModels.MDL_MAGNET_MODULE));
		setModelTexture(TEX);
		this.scale = 0.003f;
	}
	
	@Override
	protected void renderAtOrigin(TileEntity te) {
		GL11.glRotated(90, 0, 1, 0);
		super.renderAtOrigin(te);
	}

}
