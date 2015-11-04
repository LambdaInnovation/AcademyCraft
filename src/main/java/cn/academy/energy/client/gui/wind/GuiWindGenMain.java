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
package cn.academy.energy.client.gui.wind;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.energy.block.wind.ContainerWindGenMain;
import cn.academy.energy.block.wind.TileWindGenMain;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.cgui.gui.LIGui;
import cn.lambdalib.cgui.gui.LIGuiContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.annotations.GuiCallback;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.loader.EventLoader;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.helper.Font.Align;

/**
 * @author WeAthFolD
 */
public class GuiWindGenMain extends LIGuiContainer {
	
	public static LIGui loaded;
	static {
		try {
			loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/wind_main.xml"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	TileWindGenMain tile;
	
	Widget main;

	public GuiWindGenMain(ContainerWindGenMain c) {
		super(c);
		tile = c.tile;
		
		initScene();
	}
	
	void initScene() {
		main = loaded.getWidget("main").copy();
		
		gui.addWidget(main);
		
		EventLoader.load(main, this);
	}
	
	@GuiCallback("disabled")
	public void onDefFrame(Widget w, FrameEvent event) {
		DrawTexture dt = DrawTexture.get(w);
		dt.enabled = !tile.complete;
		
		if(!tile.complete) {
			String text = StatCollector.translateToLocal("ac.gui.wind.structure");
			EnergyUIHelper.drawTextBox(text, 10, -40, 20, 233333, Align.CENTER);
		} else if(!tile.noObstacle) {
			String text = StatCollector.translateToLocal("ac.gui.wind.obstacle");
			EnergyUIHelper.drawTextBox(text, 10, -40, 20, 233333, Align.CENTER);
		}
	}

}
