package cn.academy.ability.client.skilltree;

import net.minecraft.util.StatCollector;
import cn.academy.ability.developer.DeveloperType;

public class SkillTreeLocal {
	
	public static String levelDesc(int level) {
		return StatCollector.translateToLocal("ac.ability.level" + level);
	}
	
	public static String acquiredProg(float skillexp) {
		return StatCollector.translateToLocalFormatted("ac.skill_tree.acquireprog", skillexp * 100);
	}
	
	public static String upgradeTo(int level) {
		return local("uplevel", levelDesc(level));
	}
	
	public static String notAcquired() {
		return local("not_acquired");
	}
	
	public static String acquire() {
		return local("acquire");
	}
	
	public static String unknownSkill() {
		return local("unknown_skill");
	}
	
	public static String unknown() {
		return local("unknown");
	}
	
	public static String machineType(DeveloperType type) {
		return local("type_" + type.toString().toLowerCase());
	}
	
	public static String local(String key) {
		return StatCollector.translateToLocal("ac.skill_tree." + key);
	}
	
	public static String local(String key, Object ...args) {
		return StatCollector.translateToLocalFormatted("ac.skill_tree." + key, args);
	}
	
}
