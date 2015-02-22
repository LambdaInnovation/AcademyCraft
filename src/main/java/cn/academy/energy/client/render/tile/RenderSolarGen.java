/**
 * 
 */
package cn.academy.energy.client.render.tile;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACModels;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@SideOnly(Side.CLIENT)
public class RenderSolarGen extends TileEntitySpecialRenderer {

	IModelCustom model = ACModels.MDL_SOLAR;
	ResourceLocation tex = ACClientProps.TEX_MDL_SOLAR;
	
	@Override
	public void renderTileEntityAt(TileEntity te, double x,
			double y, double z, float u) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + .5, y, z + .5);
		double scale = 0.018;
		GL11.glScaled(scale, scale, scale);
		RenderUtils.loadTexture(tex);
		model.renderAll();
		GL11.glPopMatrix();
	}
	
}
