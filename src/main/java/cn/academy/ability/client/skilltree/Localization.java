package cn.academy.ability.client.skilltree;

import net.minecraft.util.StatCollector;
import cn.academy.ability.developer.DeveloperType;

class Localization {
	static String levelDesc(int level) {
		return StatCollector.translateToLocal("ac.ability.level" + level);
	}
	
	static String acquiredProg(float skillexp) {
		return StatCollector.translateToLocalFormatted("ac.skill_tree.acquireprog", skillexp * 100);
	}
	
	static String upgradeTo(int level) {
		return local("uplevel", levelDesc(level));
	}
	
	static String notAcquired() {
		return local("not_acquired");
	}
	
	static String acquire() {
		return local("acquire");
	}
	
	static String unknownSkill() {
		return local("unknown_skill");
	}
	
	static String unknown() {
		return local("unknown");
	}
	
	static String machineType(DeveloperType type) {
		return local("type_" + type.toString().toLowerCase());
	}
	
	static String local(String key) {
		return StatCollector.translateToLocal("ac.skill_tree." + key);
	}
	
	static String local(String key, Object ...args) {
		return StatCollector.translateToLocalFormatted("ac.skill_tree." + key, args);
	}
}
