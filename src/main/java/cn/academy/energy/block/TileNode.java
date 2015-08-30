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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.client.render.block.RenderDynamicBlock;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.RangedTarget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileNode extends TileInventory implements IWirelessNode, IInventory {
	
	static IFItemManager itemManager = IFItemManager.instance;
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderDynamicBlock renderer;

    protected double energy;
    
    private int updateTicker;
    
    /**
     * Client-only flag. Only *roughly* indicates whether the block is linked.
     * Used for just rendering.
     */
    public boolean enabled = false;
    
    public boolean chargingIn = false;
    
    public boolean chargingOut = false;
    
    public TileNode() {
    	super("wireless_node", 2);
    }
    
    @Override
    public void updateEntity() {
    	if(!getWorldObj().isRemote) {
    		++updateTicker;
    		if(updateTicker == 10) {
    			updateTicker = 0;
    			boolean b = WirelessHelper.isNodeLinked(this);
    			receiveSyncMessage(this, b, chargingIn, chargingOut, energy);
    		}
    		
    		updateChargeIn();
    		updateChargeOut();
    	}
    }
    
    private void updateChargeIn() {
    	ItemStack stack = this.getStackInSlot(0);
    	if(stack != null && itemManager.isSupported(stack)) {
    		//Charge into the node.
    		double req = Math.min(getBandwidth(), getMaxEnergy() - energy);
    		double pull = itemManager.pull(stack, req, false);
    		
    		chargingIn = pull != 0;
    		this.setEnergy(energy + pull);
    	} else {
    		chargingIn = false;
    	}
    }
    
    private void updateChargeOut() {
    	ItemStack stack = this.getStackInSlot(1);
    	if(stack != null && itemManager.isSupported(stack)) {
    		double cur = getEnergy();
    		if(cur > 0) {
    			cur = Math.min(getBandwidth(), cur);
    			double left = itemManager.charge(stack, cur);
    			
    			chargingOut = left != cur;
    			this.setEnergy(getEnergy() - (cur - left));
    		}
    	} else {
    		chargingOut = false;
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
    public double getBandwidth() {
        return getType().bandwidth;
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

    String name = "Unnamed";
    
	public String getNodeName() {
		return name;
	}

	public void setNodeName(String name) {
		this.name = name;
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	public void receiveSyncMessage(
		@RangedTarget(range = 12) TileEntity te,
		@Data Boolean enabled, 
		@Data Boolean chargingIn,
		@Data Boolean chargingOut, 
		@Data Double energy) {
		this.enabled = enabled;
		this.chargingIn = chargingIn;
		this.chargingOut = chargingOut;
		this.energy = energy;
	}

	@Override
	public int getCapacity() {
		return getType().capacity;
	}

}
