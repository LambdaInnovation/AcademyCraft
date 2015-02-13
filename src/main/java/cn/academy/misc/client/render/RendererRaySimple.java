/**
 * 
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
	
	final ResourceLocation tex;
	final double ratio;
	double width = 0.3;
	
	protected DrawObject drawer;
	protected Rect rect;
	
	public RendererRaySimple(ResourceLocation _tex, double _ratio) {
		tex = _tex;
		ratio = _ratio;
		
		drawer = new DrawObject();
		drawer.addHandler(rect = new Rect());
	}
	
	public void setWidth(double w) {
		width = w;
	}

	@Override
	protected void drawAtOrigin(T ent, double len) {
		GL11.glPushMatrix(); {
			
			RenderUtils.loadTexture(tex);
			
			GL11.glTranslated(0, -width * 0.5, 0);
			rect.map.set(0, 0, len / width / ratio, 1);
			rect.setSize(len, width);
			drawer.draw();
			
		} GL11.glPopMatrix();
	}

}
