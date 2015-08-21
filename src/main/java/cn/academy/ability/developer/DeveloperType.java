package cn.academy.ability.developer;

import cn.academy.core.AcademyCraft;
import cn.academy.energy.IFConstants;
import cn.liutils.ripple.ScriptNamespace;

public enum DeveloperType {

	PORTABLE(IFConstants.LATENCY_MK1, 0.3), 
	NORMAL(IFConstants.LATENCY_MK2, 0.7), 
	ADVANCED(IFConstants.LATENCY_MK3, 1.0);
	
	private final double bandwidth;
	public final double syncRate;
	
	DeveloperType(double _bandwidth, double _syncRate) {
		bandwidth = _bandwidth;
		syncRate = _syncRate;
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
