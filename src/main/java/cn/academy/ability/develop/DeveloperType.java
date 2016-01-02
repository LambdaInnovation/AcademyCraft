package cn.academy.ability.develop;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.energy.IFConstants;
import cn.lambdalib.ripple.ScriptNamespace;
import net.minecraft.util.ResourceLocation;

public enum DeveloperType {

    PORTABLE(IFConstants.LATENCY_MK1, 0.3, "items/developer_portable"), 
    NORMAL(IFConstants.LATENCY_MK2, 0.7, "blocks/developer_normal"), 
    ADVANCED(IFConstants.LATENCY_MK3, 1.0, "blocks/developer_advanced");
    
    private final double bandwidth;
    public final double syncRate;
    public final ResourceLocation texture;
    
    DeveloperType(double _bandwidth, double _syncRate, String _tex) {
        bandwidth = _bandwidth;
        syncRate = _syncRate;
        texture = Resources.getTexture(_tex);
    }
    
    public ScriptNamespace script() {
        return AcademyCraft.getScript().at("ac.developer." + toString().toLowerCase());
    }
    
    public double getEnergy() {
        return script().getDouble("energy");
    }

    // Consumption per stimulation
    public double getCPS() {
        return script().getDouble("cps");
    }
    
    public double getBandwidth() {
        return bandwidth;
    }

    // Tick per stimulation
    public int getTPS() {
        return script().getInteger("tps");
    }

}
