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
package cn.academy.misc.tutorial.client;

import java.util.Collection;

import cn.academy.core.AcademyCraft;
import cn.academy.misc.tutorial.ACTutorial;
import cn.lambdalib.cgui.gui.LIGui;
import cn.lambdalib.cgui.gui.LIGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.annotations.GuiCallback;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ElementList;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Tint;
import cn.lambdalib.cgui.gui.component.Transform.HeightAlign;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.MouseDownEvent;
import cn.lambdalib.cgui.loader.EventLoader;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.vis.curve.CubicCurve;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class GuiTutorial extends LIGuiScreen {

	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/tutorial.xml"));
	}
	
	final EntityPlayer player;
	final Collection<ACTutorial> tutlist;
	
	Widget frame;
	Widget leftPart, rightPart;
	
	Widget listArea;
	
	Widget showWindow, rightWindow, centerPart, logo;

	public GuiTutorial() {
		player = Minecraft.getMinecraft().thePlayer;
		tutlist = ACTutorial.getLearned(player);
		
		initUI();
	}
	
	private void initUI() {
		frame = loaded.getWidget("frame").copy();
		
		leftPart = frame.getWidget("leftPart");
		listArea = leftPart.getWidget("list");
		
		rightPart = frame.getWidget("rightPart");
		
		showWindow = rightPart.getWidget("showWindow");
		rightWindow = rightPart.getWidget("rightWindow");
		centerPart = rightPart.getWidget("centerPart");
		logo = rightPart.getWidget("logo");
		
		showWindow.transform.doesDraw = false;
		rightWindow.transform.doesDraw = false;
		centerPart.transform.doesDraw = false;
		
		rebuildList(tutlist);
		EventLoader.load(frame, this);
		
		/* Start animation controller */ {
			CubicCurve alphaCurve = new CubicCurve();
			alphaCurve.addPoint(0, 0);
			alphaCurve.addPoint(0.2, 0);
			alphaCurve.addPoint(0.5, 1);
			alphaCurve.addPoint(1.4, 1);
			alphaCurve.addPoint(1.7, 0);
			long start = GameTimer.getAbsTime();
			
			DrawTexture tex = DrawTexture.get(logo);
			tex.color.a = 0;
			
			logo.listen(FrameEvent.class, (w, event) -> 
			{
				double dt = (GameTimer.getAbsTime() - start) / 1000.0;
				if(dt > 1.7) {
					w.dispose();
				}
				tex.color.a = alphaCurve.valueAt(dt);
			});
		}
		
		gui.addWidget("frame", frame);
	}
	
	private void rebuildList(Collection<ACTutorial> list) {
		listArea.removeComponent("ElementList");
		ElementList el = new ElementList();
		for(ACTutorial t : list) {
			Widget w = new Widget();
			w.transform.setSize(72, 12);
			w.addComponent(new Tint());
			
			TextBox box = new TextBox();
			box.content = t.getTitle();
			box.size = 10;
			box.heightAlign = HeightAlign.CENTER;
			
			w.addComponent(box);
			el.addWidget(w);
		}
		listArea.addComponent(el);
	}
	
	// Search area
	@GuiCallback("leftPart/search")
	public void mouseDown(Widget w, MouseDownEvent event) {
		TextBox t = TextBox.get(w);
		if(t.color.a != 1) {
			t.color.a = 1;
			t.content = "";
		}
	}
	
	
	
}
