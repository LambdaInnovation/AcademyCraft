/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.core.config.ConfigEnv;
import cn.academy.energy.IFConstants;
import cn.lambdalib.ripple.ScriptNamespace;
import net.minecraft.util.ResourceLocation;

public enum DeveloperType {

    PORTABLE(IFConstants.LATENCY_MK1, 0.3, "items/developer_portable"), 
    NORMAL(IFConstants.LATENCY_MK2, 0.7, "blocks/developer_normal"), 
    ADVANCED(IFConstants.LATENCY_MK3, 1.0, "blocks/developer_advanced");

    private final ConfigEnv env = ConfigEnv.global.getEnv("ac.machines.developer." + this.name().toLowerCase());
    private final double bandwidth;
    public final double syncRate;
    public final ResourceLocation texture;
    
    DeveloperType(double _bandwidth, double _syncRate, String _tex) {
        bandwidth = _bandwidth;
        syncRate = _syncRate;
        texture = Resources.getTexture(_tex);
    }
    
    public double getEnergy() {
        return env.getFloat("energy");
    }

    // Consumption per stimulation
    public double getCPS() {
        return env.getFloat("cps");
    }
    
    public double getBandwidth() {
        return bandwidth;
    }

    // Tick per stimulation
    public int getTPS() {
        return env.getInt("tps");
    }

}
