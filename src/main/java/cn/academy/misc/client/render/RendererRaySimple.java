/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.client.render;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityRay;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.tess.Rect;
import cn.liutils.util.RenderUtils;

/**
 * Most simple ray rendering. Just specify one texture and we tile it all along the way.
 * @author WeathFolD
 */
public class RendererRaySimple<T extends EntityRay> extends RendererRayBase<T> {
	
	protected ResourceLocation tex;
	final double ratio;
	protected double widthFp, widthTp = 0.3;
	
	protected DrawObject drawer;
	protected Rect rect;
	
	public RendererRaySimple(ResourceLocation _tex, double _ratio) {
		tex = _tex;
		ratio = _ratio;
		
		drawer = new DrawObject();
		drawer.addHandler(rect = new Rect());
	}
	
	public void setWidthFp(double w) {
		widthFp = w;
	}
	
	public void setWidthTp(double w2) {
		widthTp = w2;
	}

	@Override
	protected void drawAtOrigin(T ent, double len, boolean firstPerson) {
		double width = firstPerson ? widthFp : widthTp;
		GL11.glPushMatrix(); {
			
			RenderUtils.loadTexture(tex);
			
			GL11.glTranslated(0, -width * 0.5, 0);
			rect.map.set(0, 0, len / width / ratio, 1);
			rect.setSize(len, width);
			drawer.draw();
			
		} GL11.glPopMatrix();
	}

}
