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

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

/**
 * A delegator renderer for Sprite and Entities that implements ISpriteProvider.
 * @author WeAthFolD
 */
public class RenderIcon extends Render {
	
	static Sprite sprite = new Sprite();

	@Override
	public void doRender(Entity ent, double x, double y, double z, float a, float b) {
		ISpriteEntity ish = (ISpriteEntity) ent;
		ish.updateSprite(sprite);
		
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glPushMatrix();
		
		if(ish.needViewOptimize()) {
			GL11.glTranslated(0, -0.2, 0);
		}
		
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
		sprite.draw();
		
		
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
