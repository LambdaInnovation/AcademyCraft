/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.render.block;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.energy.block.TilePhaseGen;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.generic.MathUtils;

/**
 * @author WeAthFolD
 */
public class RenderPhaseGen extends TileEntitySpecialRenderer {
	
	IModelCustom model;
	ResourceLocation[] textures;
	
	public RenderPhaseGen() {
		model = Resources.getModel("ip_gen");
		textures = Resources.getTextureSeq("models/ip_gen", 5);
	}

	@Override
	public void renderTileEntityAt(TileEntity te, 
		double x, double y, double z, float wtf) {
		TilePhaseGen gen = (TilePhaseGen) te;
		
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		int tid = MathUtils.clampi(0, 4, 
			(int) Math.round(4.0 * gen.getLiquidAmount() / gen.getTankSize()));
		RenderUtils.loadTexture(textures[tid]);
		model.renderAll();
		GL11.glPopMatrix();
	}

}
