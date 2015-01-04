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
	
	public static String learnAbility() {
		return local("ac.learnability");
	}
	
	public static String machineStat() {
		return local("ac.machinestat");
	}
	
	public static String curEnergy() {
		return local("ac.curenergy");
	}
	
	public static String presetSettings() {
		return local("ac.presset");
	}
	
	public static String pgLearn() {
		return local("page.adlearning");
	}
	
	public static String pgSkills() {
		return local("page.adskills");
	}
	
	private static String local(String str) {
		return StatCollector.translateToLocal(str);
	}

}
