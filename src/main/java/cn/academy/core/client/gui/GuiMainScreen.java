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
		double size = 60.0, x = w - 80, y = h - 65;
		boolean active = EventHandlerClient.isSkillEnabled();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			{ //Logo rendering
				if(!active) GL11.glColor4d(1, 1, 1, 0.5);
				RenderUtils.loadTexture(ACClientProps.TEX_LOGO_RAYS);
				HudUtils.drawTexturedModalRect(x, y, size, size);
				
				RenderUtils.loadTexture(ACClientProps.TEX_LOGO_FRAME);
				HudUtils.drawTexturedModalRect(x, y, size, size);
				
				RenderUtils.loadTexture(data.getCategory().getLogo());
				HudUtils.drawTexturedModalRect(x + size * 0.2109, y + size * 0.2578, size * 0.5157, size * 0.5157);
			} GL11.glColor4d(1, 1, 1, 1);
			
			GL11.glColor3ub((byte)255, (byte)255, (byte)255);
			if(active) { //cpbar rendering
				RenderUtils.loadTexture(ACClientProps.TEX_HUD_BAR);
				HudUtils.setTextureResolution(512, 200);
				//System.out.println("d");
				double scale = .3;
				GL11.glTranslated(w - 160, 17, 0);
				GL11.glScaled(scale, scale, 0);
				
				double tw = 443, th = 118;
				HudUtils.drawTexturedModalRect(0, 0, 35, 47, tw, th, tw, th);
				
				//CP progress
				GL11.glColor3ub((byte)102, (byte)212, (byte)253);
				double prog = (double)data.getCurrentCP() / data.getMaxCP();
				prog = .4;
				double pw = 436 * prog, ph = 28;
				HudUtils.drawTexturedModalRect(439 - pw, 2.5, 474 - pw, 5, pw, ph, pw, ph);
				
				//Level info
				GL11.glColor4d(1, 1, 1, 1);
				String str = data.getLevel().getDisplayName();
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 183, 33, 25);
				
				//CP numeric info
				str = String.format("%.0f/%.0f", data.getCurrentCP(), data.getMaxCP());
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 312, 85, 30, TrueTypeFont.ALIGN_CENTER);
			}
		} GL11.glPopMatrix();
		GL11.glColor4d(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
	}

}
