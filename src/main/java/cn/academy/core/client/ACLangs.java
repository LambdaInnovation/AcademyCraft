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
		return local("ac.preset");
	}
	
	public static String notLearned() {
		return local("ac.notlearned");
	}
	
	public static String devSyncRate() {
		return local("ac.syncrate");
	}
	
	private static String local(String str) {
		return StatCollector.translateToLocal(str);
	}

}
