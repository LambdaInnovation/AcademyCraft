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
package cn.academy.energy.block.wind;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.client.render.block.RenderWindGenMain;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.RangedTarget;
import cn.liutils.template.block.IMultiTile;
import cn.liutils.template.block.InfoBlockMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileWindGenMain extends TileInventory implements IMultiTile {
	
	// State for render
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderWindGenMain renderer;
	
	@SideOnly(Side.CLIENT)
	public long lastFrame = -1;
	@SideOnly(Side.CLIENT)
	public float lastRotation;
	
	public boolean complete;
	
	int updateWait, updateWait2;
	
	public TileWindGenMain() {
		super("windgen_main", 1);
	}

	// Spin logic
	public boolean isFanInstalled() {
		ItemStack stack = this.getStackInSlot(0);
		return stack != null && stack.getItem() == ModuleEnergy.windgenFan;
	}
	
	/**
	 * Unit: Degree per second
	 */
	@SideOnly(Side.CLIENT)
	public double getSpinSpeed() {
		return complete ? 60.0 : 0;
	}

	// InfoBlockMulti delegates
	InfoBlockMulti info = new InfoBlockMulti(this);
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		info.update();
		
		if(getWorldObj().isRemote) {
			if(++updateWait == 10) {
				updateWait = 0;
				complete = isCompleteStructure();
			}
		} else {
			if(++updateWait2 == 20) {
				updateWait2 = 0;
				this.syncTheStack(this, this.getStackInSlot(0));
			}
		}
	}
	
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		info = new InfoBlockMulti(this, tag);
	}
	
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		info.save(tag);
	}

	@Override
	public InfoBlockMulti getBlockInfo() {
		return info;
	}

	@Override
	public void setBlockInfo(InfoBlockMulti i) {
		info = i;
	}
	
	@Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
    	return slot != 0 || (stack != null && stack.getItem() == ModuleEnergy.windgenFan);
    }
	
	public boolean isCompleteStructure() {
		int[] origin = ModuleEnergy.windgenMain.getOrigin(this);
		if(origin == null)
			return false;
		
		int x = origin[0], y = origin[1] - 1, z = origin[2];
		int state = 1;
		int pillars = 0;
		
		for(; state < 2; --y) {
			Block block = worldObj.getBlock(x, y, z);
			if(state == 1) {
				if(block == ModuleEnergy.windgenPillar) {
					++pillars;
					if(pillars > WindGenerator.MAX_PILLARS)
						break;
				} else if(block == ModuleEnergy.windgenBase){
					state = 2;
				} else {
					state = 3;
				}
			}
		}
		return state == 2 && pillars >= WindGenerator.MIN_PILLARS;
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	private void syncTheStack(
		@RangedTarget(range = 16) TileEntity te, 
		@Data ItemStack stack) {
		this.setInventorySlotContents(0, stack);
	}
	
}
