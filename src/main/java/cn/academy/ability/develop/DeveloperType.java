package cn.academy.ability.develop;

import cn.academy.Resources;
import cn.academy.energy.IFConstants;
import net.minecraft.util.ResourceLocation;

public enum DeveloperType {
    //--------------------------| syncRate| energy | tps | cps |------------------------------
    PORTABLE(IFConstants.LATENCY_MK1, 0.3,  10000,   25,  750, "items/developer_portable_empty"),
    NORMAL  (IFConstants.LATENCY_MK2, 0.7,  50000,   20,  700, "blocks/dev_normal"),
    ADVANCED(IFConstants.LATENCY_MK3, 1.0,  200000,  15,  600, "blocks/dev_advanced");

    private final double bandwidth;
    public final double syncRate;
    public final ResourceLocation texture;
    public final double energy, cps;
    public final int tps;
    
    DeveloperType(double _bandwidth, double _syncRate,
                  double _energy, int _tps, double _cps,
                  String _tex) {
        bandwidth = _bandwidth;
        syncRate = _syncRate;
        energy = _energy;
        tps = _tps;
        cps = _cps;

        texture = Resources.getTexture(_tex);
    }
    
    public double getEnergy() {
        return energy;
    }

    // Consumption per stimulation
    public double getCPS() {
        return cps;
    }
    
    public double getBandwidth() {
        return bandwidth;
    }

    // Tick per stimulation
    public int getTPS() {
        return tps;
    }

}