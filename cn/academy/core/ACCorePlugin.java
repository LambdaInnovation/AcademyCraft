/**
 * 
 */
package cn.academy.core;

import java.util.Map;

import cpw.mods.fml.relauncher.FMLCorePlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * ASM的入口
 * @author WeathFolD
 *
 */
public class ACCorePlugin implements IFMLLoadingPlugin {

	public ACCorePlugin() {
	}

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
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
