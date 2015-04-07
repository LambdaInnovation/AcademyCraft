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
package cn.academy.energy.block.tile.impl;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.block.tile.base.TileNodeBase;
import cn.academy.energy.client.render.tile.RenderNode;
import cn.academy.energy.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileNode extends TileNodeBase implements IInventory, IEnergySink {
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderNode renderer;

	public TileNode() {
		super(10000, 128, 30);
	}
	
	String channelToLoad;

	@Override
	public void updateEntity() {
		super.updateEntity();
		if(channelToLoad != null) {
			WirelessSystem.registerNode(this, channelToLoad);
			channelToLoad = null;
		}
		
		if(battery != null) {
			this.energy -= EnergyUtils.tryCharge(battery, (int) Math.min(this.energy, this.getLatency()), false);
		}
	}
	
	@Override
	protected void init() {
		super.init();
		if(!worldObj.isRemote) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		}
	}
	
	@Override
	protected void onUnload() {
		super.onUnload();
		if(!worldObj.isRemote) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
		}
	}
	
	//---Inventory part
	ItemStack battery; //battery slot
	
	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int var1) {
		return battery;
	}

	@Override
	public ItemStack decrStackSize(int slot, int count) {
		if(battery.stackSize > 1) {
			--battery.stackSize;
			return battery;
		}
		ItemStack toRet = battery;
		battery = null;
		return toRet;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		return battery;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		battery = stack;
	}

	@Override
	public String getInventoryName() {
		return "ac_node";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return player.getDistanceSq(xCoord + .5, yCoord + .5, zCoord + .5) < 12;
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return EnergyUtils.isElecItem(stack);
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);
        NBTTagCompound tmp = tag.getCompoundTag("battery");
        if(tmp != null)
        	battery = ItemStack.loadItemStackFromNBT(tmp);
        if(tag.getBoolean("connected")) {
        	channelToLoad = tag.getString("netChannel");
        }
    }

    @Override
	public void writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);
        if(battery != null) {
        	NBTTagCompound tmp = new NBTTagCompound();
        	battery.writeToNBT(tmp);
        	tag.setTag("battery", tmp);
        }
        String chan = WirelessSystem.getTileChannel(this);
        if(chan != null) {
        	tag.setBoolean("connected", true);
        	tag.setString("netChannel", chan);
        }
    }

	@Override
	public double getSearchRange() {
		return 48;
	}

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter,
			ForgeDirection direction) {
		return true;
	}

	@Override
	public double getDemandedEnergy() {
		return maxEnergy - energy;
	}

	@Override
	public double injectEnergy(ForgeDirection directionFrom, double amount, double volts) {
		double need = Math.min(maxEnergy - energy, amount);
		energy += need;
		return amount - need;
	}

	@Override
	public int getSinkTier() {
		return 2;
	}

}
