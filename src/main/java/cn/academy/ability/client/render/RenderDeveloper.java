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
package cn.academy.ability.client.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.liutils.util.client.RenderUtils;

/**
 * @author WeAthFolD
 */
public class RenderDeveloper extends TileEntitySpecialRenderer {
	
	IModelCustom model;
	ResourceLocation texture;
	
	public RenderDeveloper() {
		model = Resources.getModel("developer");
		texture = Resources.getTexture("models/developer");
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x,
			double y, double z, float partialTicks) {
		final float scale = 0.5f;
		
		GL11.glPushMatrix(); {
			
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glTranslated(x, y, z);
			RenderUtils.loadTexture(texture);
			GL11.glScalef(scale, scale, scale);
			model.renderAll();
			GL11.glEnable(GL11.GL_CULL_FACE);
			
		} GL11.glPopMatrix();
	}

}
