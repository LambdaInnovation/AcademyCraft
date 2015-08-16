package cn.academy.ability.client.skilltree;

import net.minecraft.util.StatCollector;

class Localization {
	static String levelDesc(int level) {
		return StatCollector.translateToLocal("ac.ability.level" + level);
	}
	
	static String acquiredProg(float skillexp) {
		return StatCollector.translateToLocalFormatted("ac.skill_tree.acquireprog", skillexp * 100);
	}
	
	static String notAcquired() {
		return local("not_acquired");
	}
	
	static String local(String key) {
		return StatCollector.translateToLocal("ac.skill_tree." + key);
	}
}
