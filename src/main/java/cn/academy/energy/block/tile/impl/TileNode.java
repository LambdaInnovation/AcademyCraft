/**
 * 
 */
package cn.academy.energy.block.tile.impl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
public class TileNode extends TileNodeBase implements IInventory {
	
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

}
