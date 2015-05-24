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
package cn.academy.core.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.core.entity.IRay;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.generic.VecUtils;

/**
 * @author WeAthFolD
 */
public class RendererRaySimple<T extends IRay> extends RendererRayBase<T> {
	
	public static final double DEFAULT_WIDTH = 0.9;
	
	public double width;
	
	ResourceLocation blendIn, tile, blendOut;
	
	public RendererRaySimple(ResourceLocation _blendIn, ResourceLocation _tile, ResourceLocation _blendOut) {
		blendIn = _blendIn;
		tile = _tile;
		blendOut = _blendOut;
		
		setWidth(DEFAULT_WIDTH);
	}
	
	public RendererRaySimple setWidth(double w) {
		width = w;
		return this;
	}

	@Override
	protected void draw(T ray, Vec3 start, Vec3 end, Vec3 dir) {
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthMask(false);

		Tessellator t = Tessellator.instance;
		
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.defaultTexUnit, 240f, 240f);
		t.setBrightness(15728880);
		Vec3 look = VecUtils.subtract(end, start).normalize();
		
		end = VecUtils.add(end, VecUtils.scalarMultiply(look, 0.8));
		start = VecUtils.add(start, VecUtils.scalarMultiply(look, -0.6));
		
		Vec3 mid1 = VecUtils.add(start, VecUtils.scalarMultiply(look, width));
		Vec3 mid2 = VecUtils.add(end, VecUtils.scalarMultiply(look, -width));
		
		GL11.glColor4d(1, 1, 1, 0.2 + 0.4 * (1 + Math.sin(Minecraft.getSystemTime() / 400.0)));
		
		RenderUtils.loadTexture(blendIn);
		this.drawBoard(start, mid1, dir, width);
		
		RenderUtils.loadTexture(tile);
		this.drawBoard(mid1, mid2, dir, width);
		
		RenderUtils.loadTexture(blendOut);
		this.drawBoard(mid2, end, dir, width);
		
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	
	public static RendererRaySimple createFromName(String name) {
		try {
			ResourceLocation[] mats = Resources.getRayTextures(name);
			return new RendererRaySimple(mats[0], mats[1], mats[2]);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
