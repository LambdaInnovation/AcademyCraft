package cn.academy.support.ic2;

import cn.academy.AcademyCraft;
import cn.academy.block.tileentity.TileGeneratorBase;
import cn.lambdalib2.registry.mc.RegTileEntity;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;

import static cn.academy.support.ic2.IC2Support.eu2if;
import static cn.academy.support.ic2.IC2Support.if2eu;

/**
 * 
 * @author KSkun
 */
@RegTileEntity
@Optional.Interface(modid = IC2Support.IC2_MODID, iface = IC2Support.IC2_IFACE)
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
    public double getDemandedEnergy() {
        return bufferSize - getEnergy();
    }

    @Override
    public int getSinkTier() {
        return 2;
    }

    @Override
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        return if2eu(addEnergy(eu2if(amount)));
    }

    @Override
    public void onLoad()
    {
        if(!getWorld().isRemote)
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
        super.onLoad();
    }
    
    @Override
    public void onChunkUnload() {
        if(!getWorld().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.onChunkUnload();
    }
    
    @Override
    public void invalidate() {
        if(!getWorld().isRemote) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.invalidate();
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter iEnergyEmitter, EnumFacing enumFacing)
    {
        return true;
    }
}