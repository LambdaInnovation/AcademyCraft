package cn.academy.block.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * You better directly extend this class rather than delegating it. Too much interfaces could be a burden.
 * <br/> P.S.Notch的脑子绝！对！秀！逗！了！
 * @author WeAthFolD
 */
public class TileInventory extends TileEntity implements IInventory {
    
    protected final ItemStack[] inventory;
    
    private final String invName;
    
    public TileInventory(String _invName, int size) {
        inventory = new ItemStack[size];
        for(int i=0;i<size;i++)
        {
            inventory[i]=ItemStack.EMPTY;
        }
        invName = _invName;
    }
    
    public ItemStack[] getInventory() {
        return inventory;
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }
    
    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (inventory[slot] != ItemStack.EMPTY) {
            ItemStack itemstack;

            if (inventory[slot].getCount() <= count) {
                itemstack = inventory[slot];
                inventory[slot] = ItemStack.EMPTY;
            } else {
                itemstack = inventory[slot].splitStack(count);

                if (inventory[slot].getCount() == 0) {
                    inventory[slot] = ItemStack.EMPTY;//setCount(0) will make it empty, maybe this code can be removed.
                }
            }
            this.markDirty();
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        if (inventory[slot] != ItemStack.EMPTY) {
            ItemStack itemstack = inventory[slot];
            inventory[slot] = ItemStack.EMPTY;
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;

        if (stack.isEmpty() && stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }

        this.markDirty();
    }
    
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        NBTTagCompound tag = nbt.getCompoundTag("inventory");
        for(int i = 0; i < inventory.length; ++i) {
            String name = "" + i;
            if(tag.hasKey(name)) {
                inventory[i] = new ItemStack(tag.getCompoundTag(name));
            }
        }
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagCompound tag = new NBTTagCompound();
        for(int i = 0; i < inventory.length; ++i) {
            if(inventory[i] != null) {
                NBTTagCompound tag2 = new NBTTagCompound();
                inventory[i].writeToNBT(tag2);
                tag.setTag("" + i, tag2);
            }
        }
        
        nbt.setTag("inventory", tag);
        return tag;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String getName() {
        return invName;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }
}