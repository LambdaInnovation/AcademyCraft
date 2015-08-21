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
	
	public static String required() {
		return local("required");
	}
	
	public static String unknownSkill() {
		return local("unknown_skill");
	}
	
	public static String unknown() {
		return local("unknown");
	}
	
	public static String learnSkill() {
		return local("learn_skill");
	}
	
	public static String machineType(DeveloperType type) {
		return local("type_" + type.toString().toLowerCase());
	}
	
	public static String estmCons(double amt) {
		return local("consumption", amt);
	}
	
	public static String progress(double amt) {
		return local("progress", amt * 100);
	}
	
	public static String progressAborted() {
		return local("aborted");
	}
	
	public static String successful() {
		return local("successful");
	}
	
	public static String ok() {
		return local("ok");
	}
	
	public static String abort() {
		return local("abort");
	}
	
	public static String aborted() {
		return local("aborted");
	}
	
	public static String energyDesc(double energy, double maxEnergy) {
		return String.format("%.0f/%.0fIF", energy, maxEnergy);
	}
	
	public static String local(String key) {
		return StatCollector.translateToLocal("ac.skill_tree." + key);
	}
	
	public static String local(String key, Object ...args) {
		return StatCollector.translateToLocalFormatted("ac.skill_tree." + key, args);
	}
	
}
