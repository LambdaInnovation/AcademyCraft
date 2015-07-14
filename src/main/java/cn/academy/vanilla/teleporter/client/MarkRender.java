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
package cn.academy.vanilla.teleporter.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.liutils.api.render.IDrawable;
import cn.liutils.util.client.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MarkRender extends Render {
	
	protected ResourceLocation[] tex = Resources.getEffectSeq("tp_mark", 7);
	protected IDrawable model = new SimpleModelBiped();

	@Override
	public void doRender(Entity ent, double x, double y, double z, float var8, float var9) {
		EntityTPMarking mark = (EntityTPMarking) ent;
		if(!mark.firstUpdated())
			return;
		
		int texID = (int) ((mark.ticksExisted / 2.5) % tex.length);
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix(); {
			GL11.glTranslated(x, y, z);
			
			GL11.glRotated(-mark.rotationYaw, 0, 1, 0);
			GL11.glScaled(-1, -1, 1);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
			Tessellator.instance.setBrightness(15728880);
			RenderUtils.loadTexture(tex[texID]);
			
			if(!mark.available) {
				GL11.glColor4d(1, 0.2, 0.2, 1);
			} else {
				GL11.glColor4d(1, 1, 1, 1);
			}
			
			model.draw();
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}
	
}