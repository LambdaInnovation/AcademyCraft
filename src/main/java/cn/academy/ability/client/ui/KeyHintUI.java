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
package cn.academy.ability.client.ui;

import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontAlign;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.ctrl.ClientController;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.Cooldown.CooldownData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.core.client.Resources;
import cn.academy.core.client.ui.ACHud;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Transform.HeightAlign;
import cn.lambdalib.cgui.gui.component.Transform.WidthAlign;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.shader.ShaderMono;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.util.key.KeyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
public class KeyHintUI extends Widget {

    @RegInitCallback
	public static void init() {
		ACHud.instance.addElement(new KeyHintUI(), 
			() -> CPData.get(Minecraft.getMinecraft().thePlayer).isActivated());
	}
	
	ResourceLocation 
		TEX_BACK = tex("back"),
		TEX_ICON_BACK = tex("icon_back"),
		TEX_KEY_LONG = tex("key_long"),
		TEX_KEY_SHORT = tex("key_short"),
		TEX_MOUSE_L = tex("mouse_left"), 
		TEX_MOUSE_R = tex("mouse_right"), 
		TEX_MOUSE_GENERIC = tex("mouse_generic");
	
	long lastFrameTime, showTime;
	double mAlpha;
	boolean canUseAbility;

    final FontOption option = new FontOption(32, FontAlign.CENTER, new Color(0xff194246));
	
	private KeyHintUI() {
		transform.alignWidth = WidthAlign.RIGHT;
		transform.alignHeight = HeightAlign.CENTER;
		transform.x = 0;
		transform.width = 140;
		transform.scale = 0.23;
		
		addDrawing();
	}
	
	private void addDrawing() {
		listen(FrameEvent.class, (w, e) -> {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			
			PresetData pData = PresetData.get(player);
			CPData cpData = CPData.get(player);
			
			canUseAbility = cpData.canUseAbility();
			
			long time = GameTimer.getTime();
			if(time - lastFrameTime > 300L) {
				showTime = time;
			}
			
			if((time - showTime) < 300L) {
				mAlpha = (time - showTime) / 300.0;
			} else {
				mAlpha = 1.0;
			}
			
			double curY = 0, yStep = 92;
			if(pData.isActive()) {
				Preset p = pData.getCurrentPreset();
				for(int i = 0; i < PresetData.MAX_KEYS; ++i) {
					Controllable c = p.getControllable(i);
					if(c != null) {
						GL11.glPushMatrix();
						GL11.glTranslated(-200, curY, 0);
						drawSingle(ClientController.getKeyMapping(i), c);
						GL11.glPopMatrix();
						curY += yStep;
					}
				}
			}
			
			lastFrameTime = time;
			GL11.glColor4d(1, 1, 1, 1);
		});
	}
	
	private void drawSingle(int keyCode, Controllable c) {
		ResourceLocation icon = c.getHintIcon();
		String text = c.getHintText();
		
		CooldownData data = Cooldown.getCooldownData(c);
		
		// Back
		RenderUtils.loadTexture(TEX_BACK);
		color4d(1, 1, 1, 1);
		HudUtils.rect(122, 0, 185, 83);

        IFont font = Resources.font();
		
		// KeyHint
		{
			double wx = 180, wy = 27;
			if(!canUseAbility || data != null) {
				color4d(0.7, 0.7, 0.7, 1);
				ShaderMono.instance().useProgram();
			}
			
			if(keyCode >= 0) {
				String name = Keyboard.getKeyName(keyCode);
				if(name.length() <= 2) { //Short preference
					drawBack(TEX_KEY_SHORT);
				} else {
					drawBack(TEX_KEY_LONG);
				}

                font.draw(name, wx, wy, option);
			} else {
				
				if(keyCode == KeyManager.MOUSE_LEFT) {
					drawBack(TEX_MOUSE_L);
				} else if(keyCode == KeyManager.MOUSE_RIGHT) {
					drawBack(TEX_MOUSE_R);
				} else {
					drawBack(TEX_MOUSE_GENERIC);
                    font.draw("" + (keyCode + 100), wx, wy, option);
				}
				
			}
			
			color4d(1, 1, 1, 1);
		}
		
		// Logo
		color4d(1, 1, 1, 1);
		RenderUtils.loadTexture(TEX_ICON_BACK);
		HudUtils.rect(216, 5, 72, 72);
		
		
		
		float prog = data == null ? 0.0f : ((float) data.getTickLeft() / data.getMaxTick());
		float alpha = prog == 0.0f ? 1.0f : 0.4f;
		
		final double ICON_SIZE = 62;
		color4d(1, 1, 1, alpha);
		RenderUtils.loadTexture(icon);
		HudUtils.rect(221, 10, ICON_SIZE, ICON_SIZE);
		
		GL20.glUseProgram(0);
		
		if(prog != 0) {
			color4d(0.6, 0.6, 0.6, .3);
			HudUtils.colorRect(221, 10 + ICON_SIZE * (1 - prog), ICON_SIZE, ICON_SIZE * prog);
		}
	}
	
	private void drawBack(ResourceLocation tex) {
		RenderUtils.loadTexture(tex);
		color4d(1, 1, 1, 1);
		HudUtils.rect(146, 10, 70, 70);
	}
	
	private void color4d(double r, double g, double b, double a) {
		GL11.glColor4d(r, g, b, a * mAlpha);
	}
	
	private static ResourceLocation tex(String name) {
		return Resources.getTexture("guis/key_hint/" + name);
	}
	
}
