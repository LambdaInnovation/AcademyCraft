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
package cn.academy.ability.client.skilltree;

import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glDepthMask;
import cn.academy.ability.client.skilltree.GuiSkillTree.SkillHandler;
import cn.academy.core.client.component.Glow;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.LostFocusEvent;
import cn.liutils.cgui.gui.event.LostFocusEvent.LostFocusHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.GameTimer;

/**
 * @author WeAthFolD
 */
public class WidgetSkillDesc extends Widget {
	
	static final double MARGIN = 40;
	
	final SkillHandler handler;
	Color color = new Color().setColor4i(43, 43, 43, 128);
	
	// Those are sum of elements~
	double width = 0, height = 0;
	
	public WidgetSkillDesc(SkillHandler _handler) {
		handler = _handler;
		
		transform.width = 0;
		transform.height = 0;
		
		transform.x = -46 - MARGIN;
		transform.y = -40 - MARGIN;
		
		Glow glow = new Glow();
		glow.glowSize = 10.0;
		glow.zLevel = 10.0;
		glow.color.setColor4i(255, 255, 255, 128);
		glow.writeDepth = false;
		addComponent(glow);
		
		//addComponent(new DrawTexture());
	}

	@Override
	public boolean addWidget(Widget w) {
		w.transform.x = MARGIN;
		w.transform.y = MARGIN + height;
		
		width = Math.max(width, w.transform.width);
		height += w.transform.height;
		
		return super.addWidget(w);
	}
	
	public void onAdded() {
		getGui().gainFocus(this);
		
		this.regEventHandler(new FrameEventHandler() {

			long startTime = GameTimer.getTime();
			double blendTime = 200.0;
			
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				double tw = width + MARGIN * 2,
						th = height + MARGIN * 2;
				double progress = Math.min(1.0, 
						(GameTimer.getTime() - startTime) / blendTime);
				transform.width = MathUtils.lerp(0, tw, progress);
				transform.height = MathUtils.lerp(0, th, progress);
				
				glDepthMask(false);
				color.bind();
				HudUtils.pushZLevel();
				HudUtils.zLevel = 10;
				HudUtils.colorRect(0, 0, w.transform.width, w.transform.height);
				HudUtils.popZLevel();
				glColor4d(1, 1, 1, 1);
				glDepthMask(true);
			}
		});
		
		regEventHandler(new LostFocusHandler() {

			@Override
			public void handleEvent(Widget w, LostFocusEvent event) {
				handler.deactive();
				w.dispose();
			}
			
		});
	}
	
}
