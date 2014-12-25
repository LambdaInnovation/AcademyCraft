/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.misc.entity.EntityRayFX;
import cn.liutils.api.client.render.Vertex;
import cn.liutils.api.client.util.RenderUtils;

/**
 * 一般光线效果的渲染器，需要提供贴图。
 * 注意光线方向和贴图u轴平铺方向是同一个（也就是请横着画图）
 * @author WeathFolD
 */
public class RendererRay extends Render {
	
	protected ResourceLocation[] textures;
	protected double ratio = 2.0; //宽高比
	protected double width = 1.0; //宽度
	protected int frameRate = 1; //帧率
	protected double alpha = 1.0; //透明度
	protected boolean enableLight = true; //开启光照

	public RendererRay(double width, ResourceLocation... texs) {
		textures = texs;
	}
	
	public RendererRay setAlpha(double a) {
		alpha = a;
		return this;
	}
	
	public RendererRay disableLight() {
		enableLight = false;
		return this;
	}
	
	/**
	 * 设置贴图的宽高比。最好和图像数据一致以保证渲染效果。
	 */
	public RendererRay setRatio(double r) {
		ratio = r;
		return this;
	}
	
	public RendererRay setFrameRate(int r) {
		frameRate = r;
		return this;
	}

	@Override
	public void doRender(Entity ent, double x, double y, double z,
			float f1, float f2) {
		EntityRayFX ray = (EntityRayFX) ent;
		double hw = width * 0.5, len = ray.getLength();
		double umax = len / width / ratio;
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		if(!enableLight)
			GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glPushMatrix(); {
			Vertex[] vs = new Vertex[] {
				new Vertex(0, hw, 0, 0, 0),
				new Vertex(0, -hw, 0, 0, 1.0),
				new Vertex(0, -hw, len, umax, 1.0),
				new Vertex(0, hw, len, umax, 0.0),
				new Vertex(hw, 0, 0, 0, 0),
				new Vertex(-hw, 0, 0, 0, 1.0),
				new Vertex(-hw, 0, len, umax, 1.0),
				new Vertex(hw, 0, len, umax, 0.0)
			};
			
			GL11.glTranslated(x, y, z);
			GL11.glRotated(ray.rotationYaw, 0, 1, 0);
			GL11.glRotated(ray.rotationPitch, -1, 0, 0);
			
			RenderUtils.loadTexture(getTexture(ray));
			GL11.glColor4d(1.0, 1.0, 1.0, alpha);
			Tessellator t = Tessellator.instance;
			t.startDrawingQuads();
			if(!enableLight) { 
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
				t.setBrightness(15728880);
			}
			for(Vertex v : vs)
				v.addTo(t);
			t.draw();
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	
	protected ResourceLocation getTexture(EntityRayFX ray) {
		return textures[(ray.ticksExisted / frameRate) % textures.length];
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
