package cn.academy.ability.developer;

import cn.academy.core.AcademyCraft;
import cn.liutils.ripple.ScriptNamespace;

public enum DeveloperType {

	PORTABLE, NORMAL, ADVANCED;
	
	public ScriptNamespace script() {
		return AcademyCraft.getScript().at("ac.developer.");
	}
	
	public double getEnergy() {
		return script().getDouble("energy");
	}
	
	public double getCPS() {
		return script().getDouble("cps");
	}
	
	public int getTPS() {
		return script().getInteger("tps");
	}

}
