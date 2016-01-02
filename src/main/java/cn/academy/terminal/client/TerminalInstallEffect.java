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
package cn.academy.terminal.client;

import cn.academy.core.ModuleCoreClient;
import cn.lambdalib.cgui.gui.CGui;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.IGuiEventHandler;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.auxgui.AuxGui;
import cn.lambdalib.util.key.KeyManager;
import cn.lambdalib.util.mc.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class TerminalInstallEffect extends AuxGui {
	
	static final long ANIM_LENGTH = 4000L;
	static final long WAIT = 700L;
	static final long BLEND_IN = 200L, BLEND_OUT = 200L;
	
	static WidgetContainer loaded;
	static {
		loaded = CGUIDocument.panicRead(new ResourceLocation("academy:guis/terminal_installing.xml"));
	}
	
	CGui gui = new CGui();
	
	public TerminalInstallEffect() {
		gui.addWidget("main", loaded.getWidget("main").copy());
		gui.getWidget("main/progbar").listen(FrameEvent.class, (w, e) -> {
			double prog = (double) this.getTimeActive() / ANIM_LENGTH;
			if(this.getTimeActive() >= ANIM_LENGTH + WAIT) {
				dispose();
				TerminalUI.keyHandler.onKeyUp();
				PlayerUtils.sendChat(Minecraft.getMinecraft().thePlayer, "ac.terminal.key_hint",
						KeyManager.getKeyName(ModuleCoreClient.keyManager.getKeyID(TerminalUI.keyHandler)));
			}

			if(prog > 1.0) {
				prog = 1.0;
			}
			ProgressBar.get(w).progress = prog;
		});
		
		Widget main = gui.getWidget("main");
		initBlender(main);
		for(Widget w : main.getDrawList())
			initBlender(w);
	}

	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		gui.resize(sr.getScaledWidth_double(), sr.getScaledHeight_double());
		gui.draw();
	}
	
	private void initBlender(Widget w) {
		w.listen(FrameEvent.class, new IGuiEventHandler<FrameEvent>() {
			DrawTexture tex = DrawTexture.get(w);
			TextBox text = TextBox.get(w);
			ProgressBar bar = ProgressBar.get(w);
			
			double texA, textA, barA;
			{
				if(tex != null) texA = tex.color.a;
				if(text != null) textA = text.option.color.a;
				if(bar != null) barA = bar.color.a;
			}

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				double alpha;
				long dt = getTimeActive();
				if(dt < BLEND_IN) {
					alpha = (double) (dt) / BLEND_IN;
				} else if(dt > ANIM_LENGTH) {
					alpha = Math.max(0, 1 - (double) (dt - ANIM_LENGTH) / BLEND_OUT);
				} else {
					alpha = 1;
				}
				
				DrawTexture tex = DrawTexture.get(w);
				TextBox text = TextBox.get(w);
				ProgressBar bar = ProgressBar.get(w);
				if(tex != null) tex.color.a = texA * alpha;
				if(text != null) text.option.color.a = 0.1 + 0.9 * textA * alpha;
				if(bar != null) bar.color.a = barA * alpha;
			}
		});
	}

}
