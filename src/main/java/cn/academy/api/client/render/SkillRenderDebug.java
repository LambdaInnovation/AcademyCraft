/**
 * 
 */
package cn.academy.api.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ctrl.pattern.IPattern;
import cn.liutils.api.client.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class SkillRenderDebug extends SkillRenderer {
	
	public static SkillRenderDebug instance = new SkillRenderDebug();

	private SkillRenderDebug() {
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void renderHandEffect(EntityPlayer player, IPattern pattern, HandRenderType type) {
		GL11.glPushMatrix(); {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			
			GL11.glTranslated(0.22, -.35, -0.48);
			GL11.glColor4f(1F, 1F, 1F, .5F);
			Vec3 vs[] = {
				vec(0, 0, 0), 
				vec(1, 0, 0), 
				vec(1, 0, 1), 
				vec(0, 0, 1),
				vec(0, 1, 0),
				vec(1, 1, 0),
				vec(1, 1, 1),
				vec(0, 1, 1)
			};
			//MDL_SOLAR.renderAll();
			
			GL11.glRotated(45, -1, 0, 0);
			int arr[] = {
				4, 3, 2, 1,
				5, 6, 7, 8,
				7, 3, 2, 6,
				//3, 7, 8, 4,
				//8, 6, 1, 5,
				//1, 2, 6, 5
			};
			float scale = 1F;
			GL11.glScalef(scale, scale, -scale);
			if(true) {
				Tessellator t = Tessellator.instance;
				t.startDrawingQuads();
				for(int i = 0; i < arr.length; ++i) {
					Vec3 vec = vs[arr[i] - 1];
					RenderUtils.addVertex(vec);
				}
				t.draw();
				
				GL11.glColor4f(1F, 0F, 0F, 1F);
				t.startDrawing(GL11.GL_LINES);
				t.addVertex(0, 0, 0);
				t.addVertex(.5, .5, .5);
				t.draw();
				GL11.glColor4f(0F, 1F, 0F, 1F);
				t.startDrawing(GL11.GL_LINES);
				t.addVertex(.5, .5, .5);
				t.addVertex(1, 1, 1);
				t.draw();
			}
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1F, 1F, 1F, 1F);
		} GL11.glPopMatrix();
	}
	
	@SideOnly(Side.CLIENT)
	public void renderSurroundings(EntityPlayer player, IPattern pattern) {
		GL11.glPushMatrix(); {
			GL11.glTranslated(-.5, 0, -.5);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glColor4f(1, 1, 1, 0.3F);
			RenderUtils.drawCube(1, 1, 2);
			GL11.glDisable(GL11.GL_BLEND);
		} GL11.glPopMatrix();
	}
	
	private static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}

}
