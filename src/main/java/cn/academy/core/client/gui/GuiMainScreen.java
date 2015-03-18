/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.gui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.api.event.AbilityEvent;
import cn.academy.api.event.UpdateCDEvent;
import cn.academy.core.ctrl.EventHandlerClient;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.util.ACUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.api.draw.GUIObject;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.misc.Pair;
import cn.liutils.util.render.LambdaFont;
import cn.liutils.util.render.LambdaFont.Align;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
public class GuiMainScreen extends AuxGui {
	
	@RegAuxGui
	@RegEventHandler(Bus.Forge)
	public static GuiMainScreen INSTANCE = new GuiMainScreen();
	
	private long lastInactiveTime, lastActiveTime;
	
	GUIObject.Tex logoBack, logoRays, logoFrame, logoGeom;
	
	private static LambdaFont font = ACClientProps.FONT_YAHEI_32;
	
	private String tipString;
	private long tipUpdateTime;
	
	private GuiMainScreen() {
		logoBack = new GUIObject.Tex(ACClientProps.TEX_LOGO_BACK, 0, 0, 256, 256, 0, 0, 256, 256);
		logoRays = new GUIObject.Tex(ACClientProps.TEX_LOGO_RAYS, 0, 0, 256, 256, 0, 0, 256, 256);
		logoFrame = new GUIObject.Tex(ACClientProps.TEX_LOGO_FRAME, 0, 0, 256, 256, 0, 0, 256, 256);
		logoGeom = new GUIObject.Tex(ACClientProps.TEX_LOGO_GEOM, 0, 0, 256, 256, 0, 0, 256, 256);
		logoGeom.getTransform().setPivotPt(128, 128, 0);
	}
	
	//Tip API
	/**
	 * Should be translated String
	 */
	public void updateTip(String tip) {
		tipString = tip;
		tipUpdateTime = Minecraft.getSystemTime();
	}

	@Override
	public boolean isOpen() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		return player != null && AbilityDataMain.getData(player).hasAbility();
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
			//Tip rendering
			{
				GL11.glColor4d(1, 1, 1, 0.4);
				long tdt = time - tipUpdateTime;
				if(tdt > 1000) tipString = null;
				if(tipString != null) {
					ACClientProps.FONT_YAHEI_32.draw(tipString, w / 2 - 6, h / 2 - 12, 6, Align.RIGHT);
				}
			}
			
			GL11.glPushMatrix(); { //Logo rendering
				double scale = .25;
				double mAlpha = active ? 0.8 : 0.4;
				HudUtils.setTextureResolution(256, 256);
				
				GL11.glTranslated(w - 80, h - 70, 0);
				GL11.glScaled(scale, scale, 1);
				
				GL11.glColor4d(1, 1, 1, mAlpha);
				logoBack.draw();
				
				GL11.glColor4d(1, 1, 1, (mAlpha * 1.25) * (0.7 + Math.sin(time / 900D) * 0.3));
				logoRays.draw();
				
				GL11.glColor4d(1, 1, 1, mAlpha);
				logoBack.draw();
				logoFrame.draw();
				
				RenderUtils.loadTexture(data.getCategory().getLogo());
				HudUtils.drawRect(63, 63, 129, 129);
				
				logoGeom.getTransform().setRoll(time / 1000D);
				logoGeom.draw();
			} GL11.glPopMatrix();
			RenderUtils.bindIdentity();
			
			//CPBar rendering
			if(active) { 
				lastActiveTime = time;
			} else {
				lastInactiveTime = time;
			}
			double mAlpha = active ? 
					Math.min((time - lastInactiveTime) / 300D, 1.0) : 
					Math.max((300 + lastActiveTime - time) / 300D, 0.0);
			
			if(mAlpha > 0) {
				//Cooldown
				drawCooldownBars(data, mAlpha, w, h);
				
				GL11.glColor4d(1, 1, 1, mAlpha * 0.6);
				RenderUtils.loadTexture(ACClientProps.TEX_HUD_BAR);
				HudUtils.setTextureResolution(512, 200);
				double scale = .4;
				GL11.glTranslated(w - 193, 17, 0);
				GL11.glScaled(scale, scale, 0);
				//Back
				HudUtils.drawRect(0, 0, 0, 73, 455, 127, 455, 127);
				
				//CPBar
				double prog = data.getCurrentCP() / data.getMaxCP();
				int[] cs = data.getCategory().getColorStyle();
				RenderUtils.bindColor(cs[0], cs[1], cs[2], (int) (mAlpha * 255));
				HudUtils.drawRect(439 - 436 * prog, 3, 439 - 436 * prog, 4, 436 * prog, 28, 436 * prog, 28);
				
				//CPBar glow
				double alpha = Math.max(0, (prog - 0.6) / 0.4);
				GL11.glColor4d(1, 1, 1, alpha * mAlpha);
				HudUtils.drawRect(3, 3, 3, 42, 436, 28, 436, 28);
				
				//Chip
				HudUtils.drawRect(269, 46, 478, 40, 26, 26, 26, 26);
				alpha =  0.5 + 0.5 * Math.sin(Minecraft.getSystemTime() / 500D);
				RenderUtils.bindColor(cs[0], cs[1], cs[2], (int) (alpha * mAlpha * 255)); //Chip glow light
				HudUtils.drawRect(266, 45, 474, 5, 32, 32, 32, 32);
				
				//Level
				GL11.glColor4d(1, 1, 1, mAlpha * .6);
				ACClientProps.FONT_YAHEI_32.drawAdjusted(data.getLevel().getDisplayName(), 184, 58, 20, 69);
				
				//Numeric CP
				String str = String.format("%.0f/%.0f", data.getCurrentCP(), data.getMaxCP());
				font.drawAdjusted(str, 316, 87, 22, Align.CENTER, 167);
			}
		} GL11.glPopMatrix();
		RenderUtils.bindIdentity();
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void drawCooldownBars(AbilityData data, double mAlpha, double w, double h) {
		final double len = 100;
		final double fSize = 7, step = 17;
		
		double y0 = 0;
		Iterator<Map.Entry<Integer, Pair<Integer, Long>>> iter = aliveCooldowns.entrySet().iterator();
		
		long time = Minecraft.getSystemTime();
		
		GL11.glPushMatrix();
		
		GL11.glTranslated(w - 120, h - 90, 0);
		
		while(iter.hasNext()) {
			Map.Entry<Integer, Pair<Integer, Long>> ent = iter.next();
			Pair<Integer, Long> dt = ent.getValue();
			
			SkillBase sb = data.getSkill(ent.getKey());
			if(sb != null) {
				GL11.glColor4d(1, 1, 1, mAlpha);
				String name = sb.getDisplayName();
				double prog = 1 - Math.min(1, (double)(time - dt.second) / dt.first);
				ACUtils.drawText(name, len, y0, fSize, Align.RIGHT);
				GL11.glColor4d(.3, .3, .3, mAlpha);
				HudUtils.drawModalRect(0, y0 + 10, len, 2.3);
				double l = len * prog;
				GL11.glColor4d(.8, .8, .8, mAlpha);
				HudUtils.drawModalRect(len - l, y0 + 10, l, 2.3);
				y0 -= step;
			}
			
			if(time - dt.second > dt.first) {
				iter.remove();
			}
		}
		
		GL11.glPopMatrix();
	}
	
	@SubscribeEvent
	public void catChanged(AbilityEvent.ChangeCategory event) {
		aliveCooldowns.clear();
	}
	
	@SubscribeEvent
	public void onUpdateCD(UpdateCDEvent event) {
		if(event.cd == 0)
			return;
		
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		AbilityData data = AbilityDataMain.getData(player);
		int i = data.getSkillID(event.skill);
		if(i >= 0) { //Valid skill
			Pair<Integer, Long> pair = aliveCooldowns.get(i);
			if(pair == null) {
				pair = new Pair();
				aliveCooldowns.put(i, pair);
			}
			pair.first = event.cd;
			pair.second = Minecraft.getSystemTime();
		}
	}
	
	Map<Integer, Pair<Integer, Long>> aliveCooldowns = new HashMap();

	@Override
	public boolean isForeground() {
		return false;
	}

}
