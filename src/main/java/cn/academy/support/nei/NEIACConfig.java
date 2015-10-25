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
package cn.academy.support.nei;

import cn.academy.core.AcademyCraft;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

/**
 * 
 * @author KSkun
 *
 */
public class NEIACConfig implements IConfigureNEI {

	@Override
	public void loadConfig() {
		API.registerRecipeHandler(new FusorRecipeHandler());
		API.registerUsageHandler(new FusorRecipeHandler());
		API.registerRecipeHandler(new MetalFormerRecipeHandler());
		API.registerUsageHandler(new MetalFormerRecipeHandler());
	}

	@Override
	public String getName() {
		return "AcademyCraft";
	}

	@Override
	public String getVersion() {
		return AcademyCraft.VERSION;
	}

}
