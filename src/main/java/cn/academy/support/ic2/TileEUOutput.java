package cn.academy.support.ic2;

import cn.academy.core.block.TileReceiverBase;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import static cn.academy.support.ic2.IC2Support.eu2if;
import static cn.academy.support.ic2.IC2Support.if2eu;

/**
 * 
 * @author KSkun
 */
public class TileEUOutput extends TileReceiverBase implements IEnergySource {
    
    private boolean isRegistered = false;

    public TileEUOutput() {
        super("ac_eu_output", 0, 2000, 100);
    }

    @Override
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return true;
    }

    @Override
    public double getOfferedEnergy() {
        return if2eu(energy);
    }

    @Override
    public void drawEnergy(double amount) {
        energy -= eu2if(amount);
        if(energy < 0d) energy = 0d;
    }

    @Override
    public int getSourceTier() {
        return 2;
    }
    
    @Override
    public void update() {
        if(!isRegistered && !getWorld().isRemote) {
            isRegistered = MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
        }
        super.update();
    }
    
    @Override
    public void onChunkUnload() {
        if(!isRegistered && !getWorld().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.onChunkUnload();
    }
    
    @Override
    public void invalidate() {
        if(!isRegistered && !getWorld().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.invalidate();
    }

}