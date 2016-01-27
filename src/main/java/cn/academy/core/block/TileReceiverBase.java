/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.block;

import cn.academy.core.tile.TileInventory;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.RangedTarget;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.nbt.NBTTagCompound;

/**
 * BaseClass that should be used on all energy receivers.
 * This class will automatically sync its energy field to client side.
 * @author WeAthFolD
 */
@Registrant
public class TileReceiverBase extends TileInventory implements IWirelessReceiver {
    
    int UPDATE_WAIT = 20;
    int updateTicker = 0;
    
    final double maxEnergy;
    final double bandwidth;
    
    public double energy;

    public TileReceiverBase(String name, int invSize, double max, double bwidth) {
        super(name, invSize);
        maxEnergy = max;
        bandwidth = bwidth;
    }
    
    @Override
    public void updateEntity() {
        if(!getWorldObj().isRemote) {
            if(++updateTicker == UPDATE_WAIT) {
                updateTicker = 0;
                syncEnergy(this, energy);
            }
        }
    }
    
    @Override
    public double getRequiredEnergy() {
        return maxEnergy - energy;
    }

    @Override
    public double injectEnergy(double amt) {
        double req = maxEnergy - energy;
        double give = Math.min(amt, req);
        energy += give;
        
        return amt - give;
    }
    
    public double getEnergy() {
        return energy;
    }
    
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public double getBandwidth() {
        return bandwidth;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy = tag.getDouble("energy");
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("energy", energy);
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    private static void syncEnergy(@RangedTarget(range = 15) TileReceiverBase te, @Data Double e) {
        te.energy = e;
    }

    @Override
    public double pullEnergy(double amt) {
        double a = Math.min(amt, energy);
        energy -= a;
        return a;
    }

}
