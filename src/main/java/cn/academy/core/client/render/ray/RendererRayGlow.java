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
package cn.academy.core.client.render.ray;

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
import cn.liutils.util.helper.Color;

/**
 * @author WeAthFolD
 */
public class RendererRayGlow<T extends IRay> extends RendererRayBaseGlow<T> {
	
	public static final double DEFAULT_WIDTH = 0.9;
	
	public double width;
	
	public double startFix = 0.0, endFix = 0.0; //How many units of offset does we go. Used to align with cylinder renderer.
	
	public Color color = Color.WHITE();
	
	ResourceLocation blendIn, tile, blendOut;
	
	public RendererRayGlow(ResourceLocation _blendIn, ResourceLocation _tile, ResourceLocation _blendOut) {
		blendIn = _blendIn;
		tile = _tile;
		blendOut = _blendOut;
		
		setWidth(DEFAULT_WIDTH);
	}
	
	public RendererRayGlow setWidth(double w) {
		width = w;
		return this;
	}

	@Override
	protected void draw(T ray, Vec3 start, Vec3 end, Vec3 dir) {
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glDepthMask(false);

		Tessellator t = Tessellator.instance;
		
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.defaultTexUnit, 240f, 240f);
		t.setBrightness(15728880);
		Vec3 look = VecUtils.subtract(end, start).normalize();
		
		end = VecUtils.add(end, VecUtils.scalarMultiply(look, endFix));
		start = VecUtils.add(start, VecUtils.scalarMultiply(look, startFix));
		
		Vec3 mid1 = VecUtils.add(start, VecUtils.scalarMultiply(look, width));
		Vec3 mid2 = VecUtils.add(end, VecUtils.scalarMultiply(look, -width));
		
		double preA = color.a;
		color.a = preA * ray.getAlpha();
		color.bind();
		color.a = preA;
		
		double width = this.width * ray.getWidth();
		
		RenderUtils.loadTexture(blendIn);
		this.drawBoard(start, mid1, dir, width);
		
		RenderUtils.loadTexture(tile);
		this.drawBoard(mid1, mid2, dir, width);
		
		RenderUtils.loadTexture(blendOut);
		this.drawBoard(mid2, end, dir, width);
		
		GL11.glDepthMask(true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	
	public static RendererRayGlow createFromName(String name) {
		try {
			ResourceLocation[] mats = Resources.getRayTextures(name);
			return new RendererRayGlow(mats[0], mats[1], mats[2]);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
