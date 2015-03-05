/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core;

import java.util.Map;
import java.util.Map.Entry;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * ASM的入口
 * @author WeathFolD
 *
 */
public class ACCorePlugin implements IFMLLoadingPlugin {
	
	public static boolean runtimeObfEnabled;

	public ACCorePlugin() {}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {
			"cn.academy.core.asm.APITransformerClient"
		};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		runtimeObfEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
