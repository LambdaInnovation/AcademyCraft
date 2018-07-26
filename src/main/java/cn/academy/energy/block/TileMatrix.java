package cn.academy.energy.block;

import cn.academy.core.tile.TileInventory;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.client.render.block.RenderMatrix;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author WeAthFolD
 */
@RegTileEntity
@RegTileEntity.HasRender
public class TileMatrix extends TileInventory implements IWirelessMatrix, IMultiTile {
    
    @RegTileEntity.Render
    @SideOnly(Side.CLIENT)
    public static RenderMatrix renderer;
    
    // Client-only for display
    public int plateCount;
    
    int updateTicker;

    private String placerName = "";
    
    public TileMatrix() {
        super("wireless_matrix", 4);
    }
    
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if(stack == null)
            return false;
        if (0 <= slot && slot <= 2) {
            return stack.getItem() == ModuleCrafting.constPlate;
        } else if (slot == 3) {
            return stack.getItem() == ModuleEnergy.matrixCore;
        } else {
            return false;
        }
    }
    
    //InfoBlockMulti delegation
    InfoBlockMulti info = new InfoBlockMulti(this);

    public void setPlacer(EntityPlayer player) {
        placerName = player.getCommandSenderName();
    }

    public String getPlacerName() {
        return placerName;
    }
    
    @Override
    public void updateEntity() {
        info.update();

        if(info.getSubID() == 0) {
            if(!getWorldObj().isRemote && ++updateTicker == 15) {
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
    
    public int getCoreLevel() {
        ItemStack stack = getStackInSlot(3);
        return stack == null ? 0 : stack.getItemDamage() + 1;
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