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
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import cn.liutils.api.gui.Widget;

/**
 * Base class of any subpage
 * @author WeathFolD
 */
public abstract class DevSubpage extends Widget {
	
	protected static final float
		PG_OFFSET_X = 3.5F, PG_OFFSET_Y = 34.6F,
		PG_WIDTH = 136.5F, PG_HEIGHT = 146.5F;
	
	protected final GuiDeveloper base;
	protected final String name;

	public DevSubpage(GuiDeveloper gd, String name, ResourceLocation back) {
		super(PG_OFFSET_X, PG_OFFSET_Y, PG_WIDTH, PG_HEIGHT);
		this.base = gd;
		this.name = name;
		this.initTexDraw(back, 0, 0, 273, 293);
		doesDraw = false;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal(name);
	}

}
