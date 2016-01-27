/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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
    public void updateEntity() {
        if(!isRegistered && !getWorldObj().isRemote) {
            isRegistered = !MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
        }
        super.updateEntity();
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