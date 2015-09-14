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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import cn.academy.core.AcademyCraft;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.client.render.block.RenderMatrix;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.RangedTarget;
import cn.liutils.ripple.ScriptFunction;
import cn.liutils.template.block.BlockMulti;
import cn.liutils.template.block.IMultiTile;
import cn.liutils.template.block.InfoBlockMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
@RegTileEntity
@RegTileEntity.HasRender
public class TileMatrix extends TileInventory implements IWirelessMatrix, IMultiTile {
	
	public static double 
		MAX_CAPACITY, 
		MAX_BANDWIDTH, 
		MAX_RANGE;
	
	public static void init() {
		MAX_CAPACITY = getCapacity(3, 3);
		MAX_BANDWIDTH = getBandwidth(3, 3);
		MAX_RANGE = getRange(3, 3);
	}
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMatrix renderer;
	
	int updateTicker;
	
	public TileMatrix() {
		super("wireless_matrix", 4);
	}
	
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(stack == null)
			return false;
		if(0 <= slot && slot <= 2) {
			return stack.getItem() == ModuleEnergy.constPlate;
		} else if(slot == 3) {
			return stack.getItem() == ModuleEnergy.matrixCore;
		} else {
			return false;
		}
	}
	
	//InfoBlockMulti delegation
	InfoBlockMulti info = new InfoBlockMulti(this);
	
	@Override
	public void updateEntity() {
		if(info != null)
			info.update();
		
		if(++updateTicker == 20) {
			updateTicker = 0;
			this.syncInventory();
		}
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
    public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	info = new InfoBlockMulti(this, nbt);
    }
    
	@Override
    public void writeToNBT(NBTTagCompound nbt) {
    	super.writeToNBT(nbt);
    	info.save(nbt);
    }
	
	@Override
	public int getInventoryStackLimit() {
		return 1;
	}
	
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
    	Block block = getBlockType();
    	if(block instanceof BlockMulti) {
    		return ((BlockMulti) block).getRenderBB(xCoord, yCoord, zCoord, info.getDir());
    	} else {
    		return super.getRenderBoundingBox();
    	}
    }

	//WEN
	public int getPlateCount() {
		int count = 0;
		for(int i = 0; i < 3; ++i) {
			if(this.getStackInSlot(i) != null)
				count++;
		}
		return count;
	}
	
	private static int getCapacity(int N, int L) {
		return (int) getProp("capacity", N, L);
	}
	
	private static double getBandwidth(int N, int L) {
		return getProp("bandwidth", N, L);
	}
	
	private static double getRange(int N, int L) {
		return getProp("range", N, L);
	}
	
	public int getCoreLevel() {
		ItemStack stack = getStackInSlot(3);
		return stack == null ? 0 : stack.getItemDamage() + 1;
	}
	
	@Override
	public int getCapacity() {
		int N = getPlateCount(), L = getCoreLevel();
		return getCapacity(N, L);
	}

	@Override
	public double getBandwidth() {
		int N = getPlateCount(), L = getCoreLevel();
		return getBandwidth(N, L);
	}

	@Override
	public double getRange() {
		int N = getPlateCount(), L = getCoreLevel();
		return getRange(N, L);
	}
	
	private static double getProp(String propName, int N, int L) {
		return getFunc(propName).callDouble(N, L);
	}
	
	private void syncInventory() {
		syncInventory(this, inventory[0], inventory[1], 
				inventory[2], inventory[3]);
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	private void syncInventory(
			@RangedTarget(range = 15) TileMatrix matrix,
			@Data ItemStack s0, @Data ItemStack s1, @Data ItemStack s2, @Data ItemStack s3) {
		inventory[0] = s0;
		inventory[1] = s1;
		inventory[2] = s2;
		inventory[3] = s3;
	}
	
	private static ScriptFunction getFunc(String name) {
		return AcademyCraft.getFunction("matrix." + name);
	}
}
