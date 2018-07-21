/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.ic2;

import cn.academy.core.block.TileGeneratorBase;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import static cn.academy.support.ic2.IC2Support.eu2if;
import static cn.academy.support.ic2.IC2Support.if2eu;

/**
 * 
 * @author KSkun
 */
public class TileEUInput extends TileGeneratorBase implements IEnergySink {
    
    private boolean isRegistered  = false;

    public TileEUInput() {
        super("ac_eu_input", 0, 2000, 100);
    }

    @Override
    public double getGeneration(double required) {
        return 0;
    }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
        return true;
    }

    @Override
    public double getDemandedEnergy() {
        return bufferSize - getEnergy();
    }

    @Override
    public int getSinkTier() {
        return 2;
    }

    @Override
    public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage) {
        return if2eu(addEnergy(eu2if(amount)));
    }
    
    @Override
    public void update() {
        if(!isRegistered && !getWorldObj().isRemote) {
            isRegistered = MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
        }
        super.update();
    }
    
    @Override
    public void onChunkUnload() {
        if(!isRegistered && !getWorldObj().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.onChunkUnload();
    }
    
    @Override
    public void invalidate() {
        if(!isRegistered && !getWorldObj().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.invalidate();
    }

}