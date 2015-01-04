/**
 * 
 */
package cn.academy.misc.client.render.eff;

import java.util.Random;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.Vertex;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A single billboard renderer. Designed for high customization.
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public abstract class Piece {

	protected Vec3 offset = vec();
	private double width, height;
	protected double alpha = 1.0;
	protected boolean hasLight = true;
	protected Vertex[] verts;
	protected static final Random RNG = new Random();
	protected double rotYaw, rotPitch;
	
	public Piece(double w, double h) {
		setSize(w, h);
	}
	
	public void setSize(double width, double height) {
		width *= .5;
		height *= .5;
		//Initialize verts right away, not creating every time
		verts = new Vertex[] {
			new Vertex(-width, 0, -height, 0, 1),
			new Vertex(width, 0, -height, 1, 1),
			new Vertex(width, 0, height, 1, 0),
			new Vertex(-width, 0, height, 0, 0)
		};
	}
	 
	protected static Vec3 vec() {
		return vec(0, 0, 0);
	}
	
	protected static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}
	
	public void draw() {
		onUpdate();
		Tessellator t = Tessellator.instance;
		//if(!hasLight)
		//	GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			if(!hasLight) 
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
			GL11.glTranslated(offset.xCoord, offset.yCoord, offset.zCoord);
			GL11.glRotated(rotYaw, 0, 1, 0);
			GL11.glRotated(rotPitch, 0, 0, 1);
			RenderUtils.loadTexture(getTexture());
			t.startDrawingQuads(); {
				t.setNormal(0, 1, 0);
				if(!hasLight) 
					t.setBrightness(15728880);
				for(Vertex v : verts)
					v.addTo(t);
			} t.draw();
		} GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	public abstract ResourceLocation getTexture();
	protected void onUpdate() {}
	
}
