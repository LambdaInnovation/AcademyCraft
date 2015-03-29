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
package cn.academy.energy.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class TileNode extends TileEntity implements IWirelessNode {

    protected double energy;
    
    /**
     * Client-only flag. Only *roughly* indicates whether the block is linked.
     * Used for just rendering.
     */
    @SideOnly(Side.CLIENT)
    public boolean enabled;
    
    public TileNode() {}

    @Override
    public double getMaxEnergy() {
        return getType().maxEnergy;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double value) {
        energy = value;
    }

    @Override
    public double getLatency() {
        return getType().latency;
    }

    @Override
    public double getRange() {
        return getType().range;
    }
    
    public NodeType getType() {
        return NodeType.values()[getBlockMetadata()];
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
    
    /**
     * Helper method to link a network.
     */
    public void linkNetwork(String ssid) {
        
    }

}
