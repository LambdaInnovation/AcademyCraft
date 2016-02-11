/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.AcademyCraft;
import cn.academy.core.tile.TileInventory;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.client.render.block.RenderMatrix;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cn.lambdalib.multiblock.BlockMulti;
import cn.lambdalib.multiblock.IMultiTile;
import cn.lambdalib.multiblock.InfoBlockMulti;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.RangedTarget;
import cn.lambdalib.ripple.ScriptFunction;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileMatrix extends TileInventory implements IWirelessMatrix, IMultiTile {
    
    public static double 
        MAX_CAPACITY, 
        MAX_BANDWIDTH, 
        MAX_RANGE;

    @RegInitCallback
    public static void init() {
        MAX_CAPACITY = getCapacity(3, 3);
        MAX_BANDWIDTH = getBandwidth(3, 3);
        MAX_RANGE = getRange(3, 3);
    }
    
    @RegTileEntity.Render
    @SideOnly(Side.CLIENT)
    public static RenderMatrix renderer;
    
    // Client-only for display
    public int plateCount;
    
    int updateTicker;
    
    public TileMatrix() {
        super("wireless_matrix", 4);
    }
    
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if(stack == null)
            return false;
        if(0 <= slot && slot <= 2) {
            return stack.getItem() == ModuleCrafting.constPlate;
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
        
        if(info.getSubID() != 0)
            return;
        if(!getWorldObj().isRemote && ++updateTicker == 20) {
            updateTicker = 0;
            this.syncPlates();
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
    /**
     * Server only.
     */
    public int getPlateCount() {
        int count = 0;
        for(int i = 0; i < 3; ++i) {
            if(this.getStackInSlot(i) != null)
                count++;
        }
        return count;
    }
    
    private static int getCapacity(int N, int L) {
        return (int) Math.sqrt(N) * L * 6;
    }
    
    private static double getBandwidth(int N, int L) {
        return N * L * L * 20;
    }
    
    private static double getRange(int N, int L) {
        return N * 8 * Math.sqrt(L);
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
    
    private void syncPlates() {
        syncInventory(this, getPlateCount());
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    private static void syncInventory(
            @RangedTarget(range = 15) TileMatrix matrix,
            @Data Integer plateCount) {
        matrix.plateCount = plateCount;
    }
}
