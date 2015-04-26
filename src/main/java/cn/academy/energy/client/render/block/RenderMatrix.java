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

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.energy.block.TileMatrix;
import cn.academy.generic.client.Resources;
import cn.liutils.template.block.RenderBlockMulti;
import cn.liutils.util.RenderUtils;

/**
 * @author WeAthFolD
 *
 */
public class RenderMatrix extends RenderBlockMulti {

	IModelCustom model;
	ResourceLocation texture;
	
	public RenderMatrix() {
		model = Resources.getModel("matrix");
		texture = Resources.getTexture("models/matrix");
	}
	
	@Override
	public void drawAtOrigin(TileEntity te) {
		
		TileMatrix matrix = (TileMatrix) te;
		GL11.glPushMatrix();
		
		RenderUtils.loadTexture(texture);
		drawBase(matrix);
		drawShields(matrix);
		
		GL11.glPopMatrix();
		
	}
	
	private void drawBase(TileMatrix mat) {
		model.renderPart("Main");
		model.renderPart("Core");
	}
	
	private void drawShields(TileMatrix mat) {
		int shieldCount = 3;
		long time = Minecraft.getMinecraft().getSystemTime();
		double dtheta = 360.0 / shieldCount, phase = time / 20.0;
		double htPhaseOff = 40.0;
		for(int i = 0; i < shieldCount; ++i) {
			GL11.glPushMatrix();
			
			double floatHeight = 0.1;
			GL11.glTranslated(0, floatHeight * Math.sin(time / 900.0 + htPhaseOff * i), 0);
			GL11.glRotated(phase + dtheta * i, 0, 1, 0);
			model.renderPart("Shield");
			
			GL11.glPopMatrix();
		}
	}

}
