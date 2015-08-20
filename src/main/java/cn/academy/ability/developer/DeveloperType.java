package cn.academy.ability.developer;

import cn.academy.core.AcademyCraft;
import cn.academy.energy.IFConstants;
import cn.liutils.ripple.ScriptNamespace;

public enum DeveloperType {

	PORTABLE(IFConstants.LATENCY_MK1), 
	NORMAL(IFConstants.LATENCY_MK2), 
	ADVANCED(IFConstants.LATENCY_MK3);
	
	private final double bandwidth;
	
	DeveloperType(double _bandwidth) {
		bandwidth = _bandwidth;
	}
	
	public ScriptNamespace script() {
		return AcademyCraft.getScript().at("ac.developer." + toString().toLowerCase());
	}
	
	public double getEnergy() {
		return script().getDouble("energy");
	}
	
	public double getCPS() {
		return script().getDouble("cps");
	}
	
	public double getBandwidth() {
		return bandwidth;
	}
	
	public int getTPS() {
		return script().getInteger("tps");
	}

}
