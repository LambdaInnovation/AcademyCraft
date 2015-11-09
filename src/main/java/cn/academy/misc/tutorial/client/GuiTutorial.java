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

import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslated;

import java.util.Collection;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACRenderingHelper;
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
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.loader.EventLoader;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
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
	
	static final Color GLOW_COLOR = Color.WHITE();
	
	final EntityPlayer player;
	final Collection<ACTutorial> tutlist;
	
	Widget frame;
	Widget leftPart, rightPart;
	
	Widget listArea, searchArea;
	
	Widget showWindow, rightWindow, centerPart;
	
	Widget logo0, logo1, logo2, logo3;

	public GuiTutorial() {
		player = Minecraft.getMinecraft().thePlayer;
		tutlist = ACTutorial.getLearned(player);
		
		initUI();
	}
	
	private void initUI() {
		frame = loaded.getWidget("frame").copy();
		
		leftPart = frame.getWidget("leftPart");
		listArea = leftPart.getWidget("list");
		searchArea = leftPart.getWidget("search");
		
		rightPart = frame.getWidget("rightPart");
		
		showWindow = rightPart.getWidget("showWindow");
		rightWindow = rightPart.getWidget("rightWindow");
		centerPart = rightPart.getWidget("centerPart");
		logo0 = rightPart.getWidget("logo0");
		logo1 = rightPart.getWidget("logo1");
		logo2 = rightPart.getWidget("logo2");
		logo3 = rightPart.getWidget("logo3");
		
		showWindow.transform.doesDraw = false;
		rightWindow.transform.doesDraw = false;
		centerPart.transform.doesDraw = false;
		
		rebuildList(tutlist);
		EventLoader.load(frame, this);
		
		searchArea.transform.doesDraw = listArea.transform.doesDraw = false;
		
		/* Start animation controller */ {
			blend(logo2, 0.65, 0.3);
			blend(logo0, 1.75, 0.3);
			blend(leftPart, 1.75, 0.3);
			blend(logo1, 1.3, 0.3);
			blend(logo3, 0.1, 0.3);
			blendy(logo3, 0.7, 0.4, 63, -36);
			
			long startTime = GameTimer.getAbsTime();
			logo1.listen(FrameEvent.class, (__, e) -> {
				final float ht = 5;
				final double 
					ln = 500, ln2 = 300, cl = 50, // Height and length
					b1 = 0.3, // Blend stage 1
					b2 = 0.2; // Blend stage 2
				
				glPushMatrix();
				glTranslated(logo1.transform.width / 2, logo1.transform.height / 2 + 15, 0);
				double dt = (GameTimer.getAbsTime() - startTime) / 1000.0 - 0.4;
				if(dt < 0) dt = 0;
				if(dt < b1) {
					if(dt > 0) {
						double len = MathUtils.lerp(0, ln, dt / b1);
						if(len > cl) {
							lineglow(cl, len, ht);
							lineglow(-len, -cl, ht);
						}
					}
				} else {
					double ldt = dt - b1;
					if(ldt > b2) {
						ldt = b2;
					}
					double len = ln;
					double len2 = MathUtils.lerp(ln - 2 * cl, ln2, ldt / b2);
					lineglow(ln - len2, len, ht);
					lineglow(-len, -(ln - len2), ht);
				}
				
				glPopMatrix();
				
				searchArea.transform.doesDraw = listArea.transform.doesDraw = dt > 2.3;
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
	
	private void lineglow(double x0, double x1, float ht) {
		ACRenderingHelper.drawGlow(x0, -1, x1-x0, ht-2, 5, GLOW_COLOR);
		glColor4d(1, 1, 1, 1);
		ACRenderingHelper.lineSegment(x0, 0, x1, 0, ht);
	}
	
	private void blend(Widget w, double start, double tin) {
		DrawTexture dt = DrawTexture.get(w);
		dt.color.a = 0;
		long startTime = GameTimer.getAbsTime();
		double startAlpha = dt.color.a;
		
		w.listen(FrameEvent.class, (__, e) -> 
		{
			double delta = (GameTimer.getAbsTime() - startTime) / 1000.0;
			double alpha = delta < start ? 0 : (delta - start < tin ? (delta - start ) / tin : 1);
			dt.color.a = alpha;
		});
	}
	
	private void blendy(Widget w, double start, double tin, double y0, double y1) {
		long startTime = GameTimer.getAbsTime();
		w.transform.y = y0;
		w.dirty = true;
		
		w.listen(FrameEvent.class, (__, e) ->
		{
			double delta = (GameTimer.getAbsTime() - startTime) / 1000.0;
			double lambda = delta < start ? 0 : (delta - start < tin ? (delta - start ) / tin : 1);
			w.transform.y = MathUtils.lerp(y0, y1, lambda);
			w.dirty = true;
		});
	}
	
	// Search area
	@GuiCallback("leftPart/search")
	public void mouseDown(Widget w, LeftClickEvent event) {
		TextBox t = TextBox.get(w);
		if(t.color.a != 1) {
			t.color.a = 1;
			t.content = "";
		}
	}
	
	
	
}
