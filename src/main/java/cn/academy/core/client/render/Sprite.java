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

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import cn.liutils.cgui.utils.Color;
import cn.liutils.util.RenderUtils;

/**
 * Represents an drawable sprite in origin. Always face (0, 0, -1).
 * @author WeAthFolD
 */
public final class Sprite {
	
	/**
	 * If the texture is null draw pure-colored sprite.
	 */
	public ResourceLocation texture;
	public float width = 1.0f, height = 1.0f;
	public Color color = Color.WHITE;
	public boolean hasLight = false;
	public boolean cullFace = true;
	
	public Sprite() {}
	
	public Sprite(ResourceLocation rl) {
		texture = rl;
	}
	
	public Sprite setTexture(ResourceLocation rl) {
		texture = rl;
		return this;
	}
	
	public Sprite setSize(float w, float h) {
		width = w;
		height = h;
		return this;
	}
	
	public Sprite enableLight() {
		hasLight = true;
		return this;
	}
	
	public Sprite disableCullFace() {
		cullFace = false;
		return this;
	}
	
	public Sprite setColor(Color nc) {
		color = nc;
		return this;
	}
	
	public void draw() {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		if(texture != null) {
			RenderUtils.loadTexture(texture);
		} else {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		
		if(!cullFace) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
		
		color.bind();
		Tessellator t = Tessellator.instance;
		if(hasLight) 
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.defaultTexUnit, 240f, 240f);
		float hw = width / 2, hh = height / 2;
		t.startDrawingQuads();
		t.setNormal(0, 0, -1);
		if(hasLight)
			t.setBrightness(15728886);
		t.addVertexWithUV(-hw, hh, 0, 0, 0);
		t.addVertexWithUV(-hw, -hh, 0, 0, 1);
		t.addVertexWithUV(hw, -hh, 0, 1, 1);
		t.addVertexWithUV(hw, hh, 0, 1, 0);
		t.draw();
		
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
	
}
