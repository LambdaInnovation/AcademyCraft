/**
 * 
 */
package cn.academy.ability.electro.client.render.skill;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.SkillState;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.render.piece.Piece;
import cn.liutils.api.render.piece.property.AssignTexture;
import cn.liutils.api.render.piece.property.DisableCullFace;
import cn.liutils.api.render.piece.property.DisableLight;
import cn.liutils.api.render.piece.property.Transform;
import cn.liutils.util.HudUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class RailgunPlaneEffect extends SkillRenderer {
	
	static final long ANIM_LEN = 1200; //animation length in milliseconds
	static final long DELAY = 100;
	final long time;
	
	private static Piece circle, line;
	static {
		//Setup pieces
		//TODO Maybe we should explicity load those for efficiency reason?
		circle = new Piece(1.7, 1.7);
		circle.setMapping(0, 0, 1, 0.853333333);
		new Transform(circle).setRotation(90, 0, 0);
		new AssignTexture(circle, ACClientProps.EFF_RAILGUN_PREP_CC);
		new DisableLight(circle);
				
		line = new Piece(44, 0.2);
		line.setMapping(0, 0.9, 1, 1.0);
		new Transform(line, 0, 0, -18, 0, 0, 0);
		new DisableCullFace(line);
		new DisableLight(line);
		new AssignTexture(line, ACClientProps.EFF_RAILGUN_PREP_CC);
	}

	public RailgunPlaneEffect(long beginTime) {
		time = beginTime;
	}
	
	@SideOnly(Side.CLIENT)
	public void renderHandEffect(EntityPlayer player, SkillState state, HandRenderType type) {
		if(type == HandRenderType.EQUIPPED) return;
		double dt = (Minecraft.getSystemTime() - time) % (DELAY + ANIM_LEN);
		if(dt < DELAY) return;
		dt -= DELAY;
		
		double tz = dt * dt / 3e4;
		double TRANS_TIME = ANIM_LEN * 0.2;
		double alpha = (dt < TRANS_TIME ? dt / TRANS_TIME : (dt > ANIM_LEN - TRANS_TIME ? (ANIM_LEN - dt) / TRANS_TIME : 1));
		
		//Draw a screen-filling blackout
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GLU.gluOrtho2D(0, 255, 0, 255);
		
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix(); 
		GL11.glDepthMask(false);
		GL11.glLoadIdentity();
		{
			GL11.glTranslated(0, 0, 0);
			GL11.glColor4d(0, 0, 0, 0.4 * alpha);
			HudUtils.setZLevel(1);
			HudUtils.drawModalRect(0, 0, 255, 255);
			HudUtils.setZLevel(-90);
		}
		GL11.glDepthMask(true);
		
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glMatrixMode(GL11.GL_MODELVIEW); //Restore the matrix
		
		//Draw the real effect
		
		GL11.glColor4d(1, 1, 1, alpha * 0.6);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.0f);
		GL11.glPushMatrix(); {
			GL11.glTranslated(0.8, 0.85 + tz * 0.37, tz);
			GL11.glRotated(-20.4, 1, 0, 0);
			
			drawSingleSide(7);
			
			GL11.glPushMatrix(); {
				GL11.glTranslated(-2.3, 0, 0);
				drawSingleSide(7);
			} GL11.glPopMatrix();
		} GL11.glPopMatrix();
		
		GL11.glDisable(GL11.GL_ALPHA_TEST);
	}
	
	private void drawSingleSide(int n) {
		line.draw();
		for(int i = n; i >= 0; --i) {
			double z = (4 + 6 * i * i) / 12d;
			GL11.glPushMatrix();
			GL11.glTranslated(0, 0, -z);
			circle.draw();
			GL11.glPopMatrix();
		}
	}

}
