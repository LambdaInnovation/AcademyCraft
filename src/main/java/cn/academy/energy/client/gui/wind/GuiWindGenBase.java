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
import cn.academy.energy.block.wind.ContainerWindGenBase;
import cn.academy.energy.block.wind.TileWindGenBase;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;

/**
 * @author WeAthFolD
 */
public class GuiWindGenBase extends LIGuiContainer {
	
	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/wind_base.xml"));
	}
	
	final TileWindGenBase tile;
	
	Widget main;

	public GuiWindGenBase(ContainerWindGenBase c) {
		super(c);
		
		tile = c.tile;
		initWidgets();
	}
	
	void initWidgets() {
		main = loaded.getWidget("main").copy();
		
		gui.addWidget(main);
	}

}
