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
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.client.ui.notification.INotification;
import cn.academy.ability.client.ui.notification.NotifyKnowledge;
import cn.academy.core.client.Resources;
import cn.academy.knowledge.event.KnowledgeLearnedEvent;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.utils.Color;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.VecUtils;
import cn.liutils.util.render.Font;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
public class NotifyUI extends Widget {
	
	final double KEEP_TIME = 4000;
	final double BLEND_IN_TIME = 500, SCAN_TIME = 500, BLEND_OUT_TIME = 300;
	
	ResourceLocation texture;
	
	Vec3 
		start = VecUtils.vec(420, 42, 0),
		end = VecUtils.vec(34, 42, 0);
	
	Font font = Font.font;
	
	INotification lastNotify;
	long lastReceiveTime;
	
	/**
	 * Just a buffer
	 */
	Color color = new Color();

	public NotifyUI() {
		addDrawing();
		
		texture = Resources.getTexture("guis/notification/back");
		transform.scale = 0.25f;
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public void addDrawing() {
		regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				if(lastNotify != null) {
					long dt = Minecraft.getSystemTime() - lastReceiveTime;
					GL11.glEnable(GL11.GL_BLEND);
					
					if(dt < BLEND_IN_TIME) {
						drawBack(Math.min(dt / 300.0, 1));
						
						//Draw the icon
						double iconAlpha = Math.max(0, Math.min(1, (dt - 200) / 300.0));
						drawIcon(start, iconAlpha);
						
						
					} else if(dt < SCAN_TIME + BLEND_IN_TIME) { //Slide-In stage
						
						double scanProgress = (dt - BLEND_IN_TIME) / SCAN_TIME;
						scanProgress = Math.sin(scanProgress * Math.PI / 2); //Use sin to simulation speed-down effect
						
						drawBack(1);
						drawIcon(VecUtils.lerp(start, end, scanProgress), 1);
						drawText(scanProgress);
						
					} else if(dt < KEEP_TIME - BLEND_OUT_TIME) {
						
						drawBack(1);
						drawIcon(end, 1);
						drawText(1);
						
					} else if(dt < KEEP_TIME) { 
						
						double alpha = 1 - (dt - (KEEP_TIME - BLEND_OUT_TIME)) / BLEND_OUT_TIME;
						drawBack(alpha);
						drawIcon(end, alpha);
						drawText(alpha);
						
					} else {
						//Blah, kill it
						lastNotify = null;
					}
					
					GL11.glColor4d(1, 1, 1, 1);
				}
				GL11.glEnable(GL11.GL_ALPHA_TEST);
			}
			
		});
	}
	
	private void drawText(double alpha) {
		if(alpha < 1E-1) alpha = 1E-1;
		//alpha = 0;
		color.a = alpha;
		
		font.draw(lastNotify.getTitle(), 137, 32, 38, color.asHexColor());
		font.draw(lastNotify.getContent(), 137, 81, 54, color.asHexColor());
	}
	
	private void drawBack(double alpha) {
		GL11.glColor4d(1, 1, 1, alpha);
		RenderUtils.loadTexture(texture);
		HudUtils.drawRect(517, 170);
	}
	
	private void drawIcon(Vec3 p, double alpha) {
		GL11.glColor4d(1, 1, 1, alpha);
		GL11.glPushMatrix();
		VecUtils.glTranslate(p);
		RenderUtils.loadTexture(lastNotify.getIcon());
		HudUtils.drawRect(83, 83);
		GL11.glPopMatrix();
	}
	
	private void notify(INotification n) {
		lastNotify = n;
		lastReceiveTime = Minecraft.getSystemTime();
	}
	
	@SubscribeEvent
	public void onLearned(KnowledgeLearnedEvent event) {
		if(GenericUtils.getEffectiveSide() == Side.CLIENT) {
			notify(new NotifyKnowledge(event.getKnowledge()));
		}
	}
	
}
