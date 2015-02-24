/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
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
