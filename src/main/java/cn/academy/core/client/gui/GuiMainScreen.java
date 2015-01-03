/**
 * 
 */
package cn.academy.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.TextUtils;
import cn.liutils.api.client.TrueTypeFont;
import cn.liutils.api.client.gui.AuxGui;
import cn.liutils.api.client.util.HudUtils;
import cn.liutils.api.client.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class GuiMainScreen extends AuxGui {
	
	public static GuiMainScreen INSTANCE = new GuiMainScreen();
	private long lastInactiveTime, lastActiveTime;
	
	private GuiMainScreen() {}

	@Override
	public boolean isOpen() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		return player != null && AbilityDataMain.getData(player).hasLearned();
	}

	@Override
	public void draw(ScaledResolution sr) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		AbilityData data = AbilityDataMain.getData(player);
		double w = sr.getScaledWidth_double(), h = sr.getScaledHeight_double();
		double size = 80.0, x = w - 80, y = h - 65;
		boolean active = EventHandlerClient.isSkillEnabled();
		long time = Minecraft.getSystemTime();
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			GL11.glPushMatrix(); { //Logo rendering
				double scale = .25;
				double mAlpha = active ? 0.8 : 0.4;
				GL11.glColor4d(1, 1, 1, mAlpha);
				GL11.glTranslated(w - 80, h - 70, 0);
				GL11.glScaled(scale, scale, 1);
				RenderUtils.loadTexture(ACClientProps.TEX_LOGO_BACK);
				HudUtils.drawTexturedModalRect(0, 0, 256, 256);
				
				GL11.glColor4d(1, 1, 1, mAlpha * 0.5 * (0.3 + Math.sin(time / 900D) * 0.7));
				RenderUtils.loadTexture(ACClientProps.TEX_LOGO_RAYS);
				HudUtils.drawTexturedModalRect(0, 0, 256, 256);
				
				GL11.glColor4d(1, 1, 1, mAlpha);
				HudUtils.setTextureResolution(256, 256);
				RenderUtils.loadTexture(ACClientProps.TEX_LOGO_FRAME);
				HudUtils.drawTexturedModalRect(0, 0, 256, 256);
				
				RenderUtils.loadTexture(data.getCategory().getLogo());
				HudUtils.drawTexturedModalRect(63, 63, 129, 129);
				
				GL11.glPushMatrix(); { //Rotating ray
					GL11.glTranslated(128, 128, 0);
					GL11.glRotated(time / 1000D, 0, 0, 1);
					GL11.glTranslated(-128, -128, 0);
					RenderUtils.loadTexture(ACClientProps.TEX_LOGO_GEOM);
					HudUtils.drawTexturedModalRect(0, 0, 256, 256);
				} GL11.glPopMatrix();
			} GL11.glPopMatrix();
			GL11.glColor4d(1, 1, 1, 1);
			
			if(active) { //cpbar rendering
				lastActiveTime = time;
			} else {
				lastInactiveTime = time;
			}
//			double masterAlpha = Math.max(Math.min((time - lastInactiveTime) / 300D, 1.0),
//					Math.min((time - lastActiveTime) / 300D, 1.0));
			double masterAlpha = active ? 
					Math.min((time - lastInactiveTime) / 300D, 1.0) : 
					Math.max((300 + lastActiveTime - time) / 300D, 0.0);
			
			if(masterAlpha > 0) {
				GL11.glColor4d(1, 1, 1, masterAlpha * 0.6);
				RenderUtils.loadTexture(ACClientProps.TEX_HUD_BAR);
				HudUtils.setTextureResolution(512, 200);
				double scale = .4;
				GL11.glTranslated(w - 193, 17, 0);
				GL11.glScaled(scale, scale, 0);
				//Back
				HudUtils.drawTexturedModalRect(0, 0, 0, 73, 455, 127, 455, 127);
				
				//CPBar
				double prog = data.getCurrentCP() / data.getMaxCP();
				prog = .8;
				GL11.glColor4d(33 / 255D, 111 / 255D, 137 / 255D, masterAlpha);
				HudUtils.drawTexturedModalRect(439 - 436 * prog, 3, 439 - 436 * prog, 4, 436 * prog, 28, 436 * prog, 28);
				//CPBar glow
				double alpha = Math.max(0, (prog - 0.6) / 0.4);
				GL11.glColor4d(1, 1, 1, alpha * masterAlpha);
				HudUtils.drawTexturedModalRect(3, 3, 3, 42, 436, 28, 436, 28);
				
				//Chip
				HudUtils.drawTexturedModalRect(269, 46, 478, 40, 26, 26, 26, 26);
				alpha =  0.5 + 0.5 * Math.sin(Minecraft.getSystemTime() / 500D);
				GL11.glColor4d(1, 1, 1, alpha * masterAlpha); //Chip glow light
				HudUtils.drawTexturedModalRect(266, 46, 474, 5, 32, 32, 32, 32);
				
				//Level
				GL11.glColor4d(1, 1, 1, masterAlpha * .6);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, data.getLevel().getDisplayName(), 184, 60, 21);
				
				//Numeric CP
				String str = String.format("%.0f/%.0f", data.getCurrentCP(), data.getMaxCP());
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 316, 88, 25, TrueTypeFont.ALIGN_CENTER);
			}
		} GL11.glPopMatrix();
		GL11.glColor4d(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
	}

}
