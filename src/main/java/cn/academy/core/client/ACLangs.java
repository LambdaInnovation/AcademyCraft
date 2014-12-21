/**
 * 
 */
package cn.academy.core.client;

import net.minecraft.util.StatCollector;

/**
 * Generic language translations
 * @author WeathFolD
 */
public class ACLangs {

	public static String presetPrefix() {
		return local("ac.preset.name");
	}
	
	private static String local(String str) {
		return StatCollector.translateToLocal(str);
	}

}
