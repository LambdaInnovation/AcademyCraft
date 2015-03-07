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
package cn.academy.energy.client.render.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACModels;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.liutils.template.client.render.block.RenderTileDirMulti;
import cn.liutils.util.RenderUtils;

public class RenderMatrix  extends RenderTileDirMulti {
	
	IModelCustom model = ACModels.MDL_GRID;
	ResourceLocation tex = ACClientProps.TEX_MDL_GRID;
	
	@Override
	public void renderAtOrigin(TileEntity te) {
		TileMatrix tm = (TileMatrix) te;
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			GL11.glTranslated(-1, 0, -1);
			double scale = 0.22;
			GL11.glScaled(scale, scale, scale);
			RenderUtils.loadTexture(tex);
			
			GL11.glDepthMask(true);
			model.renderPart("base");
			
			GL11.glPushMatrix(); {
				GL11.glTranslated(0, 6.3, 0);
				drawCube(tm.isLoaded);
			} GL11.glPopMatrix();
			
			GL11.glDepthMask(false);
			
			RenderUtils.loadTexture(tex);
			model.renderPart("plate");
			
		} GL11.glPopMatrix();
	}
	
	private void drawCube(boolean load) {
		GL11.glTranslated(0, 0.6 * Math.sin(Minecraft.getSystemTime() / 400D), 0);
		if(load) {
			GL11.glRotated(Minecraft.getSystemTime() / 25D, 1, 1, 1);
			GL11.glRotated(Minecraft.getSystemTime() / 50D, 2, 0, 1);
		} else {
			GL11.glTranslated(0, -1.5, 0);
		}
		final double size = 3.2, hs = size * 0.5;
		GL11.glTranslated(-hs, -hs, -hs);
		GL11.glColor4d(1, 1, 1, 0.7);
		RenderUtils.loadTexture(ACClientProps.TEX_MDL_GRID_BLOCK);
		RenderUtils.drawCube(size, size, size);
		GL11.glColor4d(1, 1, 1, 1);
	}
	
}