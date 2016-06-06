/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop;

import cn.academy.core.Resources;
import cn.academy.energy.IFConstants;
import net.minecraft.util.ResourceLocation;

public enum DeveloperType {

    PORTABLE(IFConstants.LATENCY_MK1, 0.3, 10000, 50, 750, "items/developer_portable_empty"),
    NORMAL(IFConstants.LATENCY_MK2, 0.7, 50000, 40, 700, "blocks/developer_normal"),
    ADVANCED(IFConstants.LATENCY_MK3, 1.0, 200000, 30, 600, "blocks/developer_advanced");

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
