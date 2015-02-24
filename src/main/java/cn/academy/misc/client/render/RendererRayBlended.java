/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.client.render;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityRay;
import cn.liutils.util.RenderUtils;
import net.minecraft.util.ResourceLocation;

/**
 * Auto-blend the ray at the beginning and the end with the new texture specified.
 * The blend area must be at the left side of the texture.
 * @author WeathFolD
 */
public class RendererRayBlended<T extends EntityRay> extends RendererRaySimple<T> {
	
	protected ResourceLocation blendTex;

	public RendererRayBlended(ResourceLocation _tex, ResourceLocation _blend, double _ratio) {
		super(_tex, _ratio);
		blendTex = _blend;
	}

	@Override
	protected void drawAtOrigin(T ent, double len, boolean w) {
		double width = w ? widthFp : widthTp;
		
		double forw = width * ratio;
		if(len < 2 * forw) len = 2 * forw; //Change to safe range
		
		GL11.glPushMatrix(); {
			
			GL11.glTranslated(0, -width * 0.5, 0);
			
			RenderUtils.loadTexture(blendTex);
			//Beginning blend
			GL11.glPushMatrix();
			rect.map.set(0, 0, 1, 1);
			rect.setSize(forw, width);
			drawer.draw();
			GL11.glPopMatrix();
			
			//Ending blend
//			GL11.glPushMatrix();
//			GL11.glTranslated(0, 0, len);
//			GL11.glScaled(1, 1, -1);
//			rect.map.set(0, 0, 1, 1);
//			drawer.draw();
//			GL11.glPopMatrix();
			
			RenderUtils.loadTexture(tex);
			//Real ray
			RenderUtils.loadTexture(tex);
			GL11.glTranslated(0, 0, forw);
			len = len - forw;
			rect.map.set(0, 0, len * ratio, 1);
			rect.setSize(len, width);
			drawer.draw();
			
		} GL11.glPopMatrix();
	}
	
}
