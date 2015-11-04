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
package cn.academy.core.client.render.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.client.shader.ShaderProgram;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
@Registrant
public class ShaderMask extends ShaderProgram {
	
	public static final ShaderMask instance = new ShaderMask();
	
	public static final int MASK_TEXID = 4;
	
	private ShaderMask() {
		this.linkShader(Resources.getShader("mask.vert"), GL20.GL_VERTEX_SHADER);
		this.linkShader(Resources.getShader("mask.frag"), GL20.GL_FRAGMENT_SHADER);
		this.compile();
		this.useProgram();
		GL20.glUniform1i(this.getUniformLocation("texture"), 0);
		GL20.glUniform1i(this.getUniformLocation("mask"), MASK_TEXID);
		GL20.glUseProgram(0);
	}
	
	public void start(ResourceLocation texture) {
		this.useProgram();
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + MASK_TEXID);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		RenderUtils.loadTexture(texture);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}
	
	public void maskTexCoord(double u, double v) {
		GL13.glMultiTexCoord2d(GL13.GL_TEXTURE0 + MASK_TEXID, u, v);
	}
	
	public void end() {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + MASK_TEXID);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL20.glUseProgram(0);
	}
	
}
