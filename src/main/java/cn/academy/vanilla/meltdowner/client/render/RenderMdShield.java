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
package cn.academy.vanilla.meltdowner.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cn.academy.core.client.Resources;
import cn.academy.vanilla.meltdowner.entity.EntityMdShield;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.shader.GLSLMesh;
import cn.lambdalib.util.client.shader.ShaderSimple;
import cn.lambdalib.util.deprecated.MeshUtils;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.GameTimer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class RenderMdShield extends Render {

	GLSLMesh mesh;
	ResourceLocation texture;
	
	public RenderMdShield() {
		texture = Resources.getTexture("effects/mdshield");
		mesh = MeshUtils.createBillboard(new GLSLMesh(), -0.5, -0.5, 0.5, 0.5);
		this.shadowOpaque = 0;
	}
	
	@Override
	public void doRender(Entity _entity, double x,
			double y, double z, float a, float b) {
		if(RenderUtils.isInShadowPass()) {
			return;
		}
		
		long time = GameTimer.getTime();
		EntityMdShield entity = (EntityMdShield) _entity;
		
		// Calculate rotation
		long dt;
		if(entity.lastRender == 0) dt = 0;
		else dt = time - entity.lastRender;
		
		float rotationSpeed = MathUtils.lerpf(0.8f, 2f, Math.min(entity.ticksExisted / 30.0f, 1f));
		entity.rotation += rotationSpeed * dt;
		if(entity.rotation >= 360f) entity.rotation -= 360f;
		
		ShaderSimple.instance().useProgram();
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.05f);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix();
		
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(-entity.rotationYaw, 0, 1, 0); 
		GL11.glRotatef(entity.rotationPitch, 1, 0, 0);
		GL11.glRotatef(entity.rotation, 0, 0, 1);
		
		float size = EntityMdShield.SIZE * MathUtils.lerpf(0.2f, 1f, Math.min(entity.ticksExisted / 15.0f, 1f));
		float alpha = Math.min(entity.ticksExisted / 6.0f, 1.0f);
		
		GL11.glScalef(size, size, 1);
		
		RenderUtils.loadTexture(texture);
		mesh.draw(ShaderSimple.instance());
		
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.1f);
		GL20.glUseProgram(0);
		
		entity.lastRender = time;
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return null;
	}

}
