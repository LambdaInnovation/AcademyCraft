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

import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiScreen;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class GuiTutorial extends LIGuiScreen {

	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/tutorial.xml"));
	}
	
	Widget frame;
	Widget leftPart, rightPart;

	public GuiTutorial() {
		initUI();
	}
	
	private void initUI() {
		frame = loaded.getWidget("frame").copy();
		
		leftPart = frame.getWidget("leftPart");
		rightPart = frame.getWidget("rightPart");
		
		rightPart.getWidget("showWindow").transform.doesDraw = false;
		rightPart.getWidget("rightWindow").transform.doesDraw = false;
		rightPart.getWidget("centerPart").transform.doesDraw = false;
		
		gui.addWidget("frame", frame);
	}

}
