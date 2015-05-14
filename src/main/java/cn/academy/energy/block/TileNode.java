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

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.academy.energy.internal.WirelessSystem;
import cn.academy.generic.client.render.block.RenderDynamicBlock;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileNode extends TileInventory implements IWirelessNode, IInventory {
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderDynamicBlock renderer;

    protected double energy;
    
    private int updateTicker;
    
    /**
     * Client-only flag. Only *roughly* indicates whether the block is linked.
     * Used for just rendering.
     */
    @SideOnly(Side.CLIENT)
    public boolean enabled = true;
    
    public TileNode() {
    	super("wireless_node", 2);
    }
    
    @Override
    public void updateEntity() {
    	if(!getWorldObj().isRemote) {
    		++updateTicker;
    		if(updateTicker == 20) {
    			updateTicker = 0;
    			boolean b = WirelessSystem.isTileActive(this);
    			System.out.println("Server " + b);
    			receiveSyncMessage(b);
    		}
    	}
    }

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

    String name = "Unnamed";
    
	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public void setNodeName(String name) {
		this.name = name;
	}
	
	//TODO: Limit the sending range, or this will send a huge bunch of useless data!
	
	@SuppressWarnings("unused")
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	public void receiveSyncMessage(@Data Boolean enabled) {
		if(this == null) {
			System.err.println("this is null!");
			return;
		}
		
		this.enabled = enabled;
	}

}
