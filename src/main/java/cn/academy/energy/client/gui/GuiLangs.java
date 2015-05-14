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
package cn.academy.energy.client.gui;

import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 *
 */
public class GuiLangs {

	public static final String loadingUppercase() {
		return local("loading_uppercase");
	}
	
	public static final String loadingLowercase() {
		return local("loading_lowercase");
	}
	
	private static final String local(String name) {
		return StatCollector.translateToLocal("ac.gui." + name + ".desc");
	}
	
}
