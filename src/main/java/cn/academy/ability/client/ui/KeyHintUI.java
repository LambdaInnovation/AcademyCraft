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

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.PresetData;
import cn.academy.ability.api.PresetData.Preset;
import cn.academy.ability.api.ctrl.ClientHandler;
import cn.academy.core.client.Resources;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Transform.HeightAlign;
import cn.liutils.cgui.gui.component.Transform.WidthAlign;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;
import cn.liutils.util.helper.GameTimer;
import cn.liutils.util.helper.KeyManager;

/**
 * TODO: I guess we should draw overload in the area of skill icons as well
 * @author WeAthFolD
 */
public class KeyHintUI extends Widget {
	
	ResourceLocation 
		TEX_BACK = tex("back"),
		TEX_ICON_BACK = tex("icon_back"),
		TEX_KEY_LONG = tex("key_long"),
		TEX_KEY_SHORT = tex("key_short"),
		TEX_MOUSE_L = tex("mouse_left"), 
		TEX_MOUSE_R = tex("mouse_right"), 
		TEX_MOUSE_GENERIC = tex("mouse_generic");
	
	Font font = Font.font;
	
	long lastFrameTime, showTime;
	double mAlpha;
	
	public KeyHintUI() {
		transform.alignWidth = WidthAlign.RIGHT;
		transform.alignHeight = HeightAlign.CENTER;
		transform.x = 0;
		transform.width = 140;
		transform.scale = 0.23;
		
		addDrawing();
	}
	
	private void addDrawing() {
		this.regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				PresetData pData = PresetData.get(Minecraft.getMinecraft().thePlayer);
				
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
							drawSingle(ClientHandler.getKeyMapping(i), c);
							GL11.glPopMatrix();
							curY += yStep;
						}
					}
				}
				
				lastFrameTime = time;
				GL11.glColor4d(1, 1, 1, 1);
			}
			
		});
	}
	
	private void drawSingle(int keyCode, Controllable c) {
		ResourceLocation icon = c.getHintIcon();
		String text = c.getHintText();
		
		// Back
		RenderUtils.loadTexture(TEX_BACK);
		color4d(1, 1, 1, 1);
		HudUtils.rect(122, 0, 185, 83);
		
		// KeyHint
		{
			double wx = 184, wy = 27;
			
			if(keyCode >= 0) {
				String name = Keyboard.getKeyName(keyCode);
				if(name.length() <= 2) { //Short preference
					drawBack(TEX_KEY_SHORT);
				} else {
					drawBack(TEX_KEY_LONG);
				}
				
				font.draw(name, wx, wy, 32, 0x194246, Align.CENTER);
			} else {
				
				if(keyCode == KeyManager.MOUSE_LEFT) {
					drawBack(TEX_MOUSE_L);
				} else if(keyCode == KeyManager.MOUSE_RIGHT) {
					drawBack(TEX_MOUSE_R);
				} else {
					drawBack(TEX_MOUSE_GENERIC);
					font.draw("" + (keyCode + 100), wx, wy, 32, 0x194246, Align.CENTER);
				}
				
			}
		}
		
		// Logo
		color4d(1, 1, 1, 1);
		RenderUtils.loadTexture(TEX_ICON_BACK);
		HudUtils.rect(216, 5, 72, 72);
		
		RenderUtils.loadTexture(icon);
		HudUtils.rect(221, 10, 62, 62);
		
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
