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
package cn.academy.knowledge.client.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.knowledge.Knowledge;
import cn.academy.knowledge.KnowledgeData;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.LIGui;
import cn.lambdalib.cgui.gui.LIGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ElementList;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Tint;
import cn.lambdalib.cgui.gui.component.VerticalDragBar;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class LifeRecordUI extends LIGuiScreen {
	
	static final LIGui loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/life_record.xml"));
	
	final Color temp = new Color();
	
	//L stands for Learned Knowledge area
	static final double L_START_X = 27, L_SIZE = 80, L_STEP = L_SIZE + 15;
	static final int L_COLUMNS = 6;
	
	final KnowledgeData data;

	public LifeRecordUI(KnowledgeData _data) {
		data = _data;
		init();
	}
	
	private void init() {
		Widget back = loaded.getWidget("back").copy();
		
		final ElementList list = new ElementList();
		
		// Build the dynamic stuffs( acquired knowledge and discovered knowledge area)
		list.addWidget(loaded.getWidget("t_title_discovered").copy());
		
		List<Knowledge> learned = new ArrayList(), discovered = new ArrayList();
		
		for(int i = 0; i < KnowledgeData.getKnowledgeCount(); ++i) {
			if(data.isLearned(i)) {
				learned.add(KnowledgeData.getKnowledge(i));
			}
			if(data.isDiscovered(i))
				discovered.add(KnowledgeData.getKnowledge(i));
		}
		
		for(Knowledge l : discovered) {
			list.addWidget(buildDiscoveredKnowledge(l));
		}
		
		list.addWidget(loaded.getWidget("t_title_acquired").copy());
		
		int count = 0;
		AcquiredRow row = new AcquiredRow();
		for(Knowledge l : learned) {
			row.add(new LearnedKnowledge(l));
			
			if(++count == L_COLUMNS) {
				count = 0;
				list.addWidget(row);
				row = new AcquiredRow();
			}
		}
		if(count != 0)
			list.addWidget(row);
		
		back.getWidget("area").addComponent(list);
		
		back.getWidget("scrollbar").listen(DraggedEvent.class, (Widget w, DraggedEvent e) -> {
			list.setProgress((int) (list.getMaxProgress() * VerticalDragBar.get(w).getProgress()));
		});
		gui.addWidget(back);
	}
	
	private void drawHoveringText(Knowledge l, double mx, double my) {
		final double SIZE_NAME = 48, SIZE_DESC = 44;
		final double W_MARGIN = 20, H_MARGIN = 10;
		
		GL11.glPushMatrix();
		
		GL11.glTranslated(mx + 60, my + 5, 1);
		
		Font font = Font.font;
		
		double len = Math.max(font.strLen(l.getName(), SIZE_NAME), font.strLen(l.getDesc(), SIZE_DESC));
		
		double x = -W_MARGIN, y = -H_MARGIN, w = len + 2 * W_MARGIN, h = 46 + SIZE_DESC + 2 * H_MARGIN;
		
		HudUtils.pushZLevel();
		HudUtils.zLevel = 0;
		
		temp.fromHexColor(0xb4343434);
		temp.bind();
		HudUtils.colorRect(x, y, w, h);
		
		temp.fromHexColor(0xa49d9d9d);
		temp.bind();
		HudUtils.drawRectOutline(x, y, w, h, 2);
		
		HudUtils.popZLevel();
		
		font.draw(l.getName(), 0, 0, SIZE_NAME, 0xdcdcdc);
		font.draw(l.getDesc(), 0, 46, SIZE_DESC, 0xb7dcdcdc);
		
		GL11.glColor4d(1, 1, 1, 1);
		
		GL11.glPopMatrix();
	}
	
	private Widget buildDiscoveredKnowledge(Knowledge l) {
		Widget ret = loaded.getWidget("t_discovered").copy();
		
		ret.addComponent(new Tint());
		
		DrawTexture.get(ret.getWidget("icon")).texture = l.getIcon();
		TextBox.get(ret.getWidget("title")).content = l.getName();
		TextBox.get(ret.getWidget("desc")).content = l.getDesc();
		
		return ret;
	}
	
	private class AcquiredRow extends Widget {
		
		double x = L_START_X;
		
		public AcquiredRow() {
			transform.setSize(614, 100);
		}
		
		public void add(Widget w) {
			w.transform.x = x;
			w.transform.y = 20;
			x += L_STEP;
			addWidget(w);
		}
		
	}
	
	private class LearnedKnowledge extends Widget {
		
		final Knowledge knowledge;
		
		public LearnedKnowledge(Knowledge l) {
			knowledge = l;
			transform.setSize(L_SIZE, L_SIZE);
			DrawTexture dt = new DrawTexture();
			dt.texture = knowledge.getIcon();
			addComponent(dt);
			
			Tint tint = new Tint();
			tint.hoverColor.setColor4d(0, 0, 0, 0.2);
			addComponent(tint);
			
			listen(FrameEvent.class, (Widget w, FrameEvent e) -> {
				if(e.hovering) {
					drawHoveringText(knowledge, e.mx, e.my);
				}
			});
		}
		
	}

}
