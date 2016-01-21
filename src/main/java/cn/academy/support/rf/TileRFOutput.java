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
package cn.academy.support.rf;

import static cn.academy.support.rf.RFSupport.if2rf;
import static cn.academy.support.rf.RFSupport.rf2if;

import cn.academy.core.block.TileReceiverBase;
import cn.lambdalib.annoreg.core.RegWithName;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

@Registrant
@RegTileEntity
@RegWithName("rf_output")
public class TileRFOutput extends TileReceiverBase implements IEnergyProvider {

    public TileRFOutput() {
        super("ac_rf_output", 0, 2000, 100);
    }
    
    @Override
    public void updateEntity() {
        super.updateEntity();
        World world = getWorldObj();
        if(!world.isRemote) {
            for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                int x = xCoord + dir.offsetX,
                    y = yCoord + dir.offsetY,
                    z = zCoord + dir.offsetZ;
                TileEntity te = world.getTileEntity(x, y, z);
                if(te instanceof IEnergyReceiver && energy > 0) {
                    IEnergyReceiver receiver = (IEnergyReceiver) te;
                    ForgeDirection rev = dir.getOpposite();
                    if(receiver.canConnectEnergy(rev)) {
                        int req = receiver.getMaxEnergyStored(rev) - receiver.getEnergyStored(rev);
                        req = Math.min(if2rf(energy), req);
                        energy -= rf2if(receiver.receiveEnergy(rev, req, false));
                    }
                }
            }
        }
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract,
            boolean simulate) {
        int e = (int) energy;
        if(!simulate) {
            energy -= rf2if(maxExtract);
            if(energy < 0) energy = 0;
        }
        return (int) Math.min(if2rf(e), maxExtract);
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return if2rf(energy);
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return if2rf(2000);
    }

}
