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

import cn.academy.misc.entity.EntityRay;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.Vertex;

/**
 * 一般光线效果的渲染器，需要提供贴图。
 * 注意光线方向和贴图u轴平铺方向是同一个（也就是请横着画图）
 * 在光线发出的时候，渲染长度长度是初始点和当前Entity点的距离。碰撞后，会有透明度渐隐效果，可以设定渐隐时间。
 * @author WeathFolD
 */
public class RendererRayAttn extends Render {
	
	protected ResourceLocation texture;
	protected double ratio = 2.0; //宽高比
	protected double width = 1.0; //宽度
	protected double alpha = 1.0; //透明度
	protected boolean enableLight = true; //开启光照
	protected int maxFadeTime = 10; //渐隐时间，in Ticks

	public RendererRayAttn(double width, ResourceLocation tex) {
		this.width = width;
		texture = tex;
	}
	
	public RendererRayAttn setAlpha(double a) {
		alpha = a;
		return this;
	}
	
	public RendererRayAttn disableLight() {
		enableLight = false;
		return this;
	}
	
	/**
	 * 设置贴图的宽高比。最好和图像数据一致以保证渲染效果。
	 */
	public RendererRayAttn setRatio(double r) {
		ratio = r;
		return this;
	}

	@Override
	public void doRender(Entity ent, double x, double y, double z,
			float f1, float f2) {
		EntityRay ray = (EntityRay) ent;
		if(ray.curX == 0 && ray.curY == 0 && ray.curZ == 0)
			return;
		double hw = width * 0.5;
		double dx = ray.curX - ray.posX, 
			dy = ray.curY - ray.posY, 
			dz = ray.curZ - ray.posZ;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
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
				new Vertex(dx, -hw + dy, dz, umax, 1.0),
				new Vertex(dx, hw + dy, dz, umax, 0.0),
				
				new Vertex(hw, 0, 0, 0, 0),
				new Vertex(-hw, 0, 0, 0, 1.0),
				new Vertex(-hw + dx, dy, dz, umax, 1.0),
				new Vertex(hw + dx, dy, dz, umax, 0.0),
				
				new Vertex(0, 0, hw, 0, 0),
				new Vertex(0, 0, -hw, 0, 1),
				new Vertex(dx, dy, -hw + dz, umax, 1),
				new Vertex(dx, dy, hw + dz, umax, 0),
			};
			
			GL11.glTranslated(x, y, z);
			
			RenderUtils.loadTexture(texture);
			double a = alpha;
			if(ray.hit) {
				alpha *= Math.max(0.0, (maxFadeTime - ray.tickAfterHit)) / maxFadeTime;
			}
			GL11.glColor4d(1.0, 1.0, 1.0, a);
			Tessellator t = Tessellator.instance;
			t.startDrawingQuads(); {
				if(!enableLight) { 
					OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
					t.setBrightness(15728880);
				}
				for(Vertex v : vs)
					v.addTo(t);
			} t.draw();
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
