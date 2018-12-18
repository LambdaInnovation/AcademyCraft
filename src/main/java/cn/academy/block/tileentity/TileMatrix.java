package cn.academy.block.tileentity;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.block.tileentity.TileInventory;
import cn.academy.worldgen.WorldGenInit;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.client.render.block.RenderMatrix;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author WeAthFolD
 */
@RegTileEntity
public class TileMatrix extends TileInventory implements IWirelessMatrix, IMultiTile, ITickable {
    
//    @RegTileEntity.Render
//    @SideOnly(Side.CLIENT)
//    public static RenderMatrix renderer;
    
    // Client-only for display
    public int plateCount;
    
    int updateTicker;

    private String placerName = "";
    
    public TileMatrix() {
        super("wireless_matrix", 4);
    }
    
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if(stack.isEmpty())
            return false;
        if (0 <= slot && slot <= 2) {
            return stack.getItem() == ACItems.constraint_plate;
        } else if (slot == 3) {
            return stack.getItem() == ACItems.mat_core;
        } else {
            return false;
        }
    }
    
    //InfoBlockMulti delegation
    InfoBlockMulti info = new InfoBlockMulti(this);

    public void setPlacer(EntityPlayer player) {
        placerName = player.getName();
    }

    public String getPlacerName() {
        return placerName;
    }
    
    @Override
    public void update() {
        info.update();

        if(info.getSubID() == 0) {
            if(!getWorld().isRemote && ++updateTicker == 15) {
                updateTicker = 0;
                this.sync();
            }
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

        placerName = nbt.getString("placer");
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        info.save(nbt);

        nbt.setString("placer", placerName);
        return nbt;
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
            return ((BlockMulti) block).getRenderBB(getPos(), info.getDir());
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
            if(!this.getStackInSlot(i).isEmpty())
                count++;
        }
        return count;
    }
    
    public int getCoreLevel() {
        ItemStack stack = getStackInSlot(3);
        return stack.isEmpty() ? 0 : stack.getItemDamage() + 1;
    }
    
    @Override
    public int getCapacity() {
        return isWorking() ? 8 * getCoreLevel() : 0;
    }

    @Override
    public double getBandwidth() {
        int L = getCoreLevel();
        return isWorking() ? L * L * 60 : 0;
    }

    @Override
    public double getRange() {
        return isWorking() ? 24 * Math.sqrt(getCoreLevel()) : 0;
    }

    private boolean isWorking() {
        return getCoreLevel() > 0 && getPlateCount() == 3;
    }
    
    private void sync() {
        NetworkMessage.sendToAllAround(
                TargetPoints.convert(this, 25),
                this, "sync", getPlateCount(), placerName);
    }

    @NetworkMessage.Listener(channel="sync", side=Side.CLIENT)
    private void hSync(int plateCount2, String placerName2) {
        plateCount = plateCount2;
        placerName = placerName2;
    }
}