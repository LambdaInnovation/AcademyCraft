/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.block;

import cn.academy.core.block.TileReceiverBase;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.PositionedSound;
import cn.academy.core.client.sound.TileEntitySound;
import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.api.MetalFormerRecipes.RecipeObject;
import cn.academy.energy.IFConstants;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cn.lambdalib.s11n.network.TargetPoints;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkMessage.NullablePar;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
public class TileMetalFormer extends TileReceiverBase implements ISidedInventory {

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        switch(side) {
            case 0:
                return new int[]{SLOT_OUT, SLOT_BATTERY};
            case 1:
                return new int[]{SLOT_IN};
            default:
                return new int[]{SLOT_BATTERY};
        }
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        return this.isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        return side == 0;
    }

    @Registrant
    @NetworkS11nType
    public enum Mode { 
        PLATE, INCISE, ETCH, REFINE; 
        
        public final ResourceLocation texture;
        Mode() {
            texture = new ResourceLocation(
                    "academy:textures/guis/icons/icon_former_" +
                    this.toString().toLowerCase() + ".png");
        }
    }; 
    
    public static final int 
        SLOT_IN = 0,
        SLOT_OUT = 1,
        SLOT_BATTERY = 2;
    
    public static final int
        WORK_TICKS = 60;
    
    public static final double
        CONSUME_PER_TICK = 13.3;
    
    // Available in both sides.
    public Mode mode = Mode.PLATE;
    public RecipeObject current;
    
    public int workCounter;
    public int updateCounter;

    public TileMetalFormer() {
        super("metal_former", 3, 3000, IFConstants.LATENCY_MK1);
    }
    
    @Override
    public void updateEntity() {
        super.updateEntity();
        
        World world = getWorldObj();
        if(!world.isRemote) {
            if(current != null) {
                // Process recipe
                if(!isActionBlocked() && this.pullEnergy(CONSUME_PER_TICK) == CONSUME_PER_TICK) {
                    ++workCounter;
                    if(workCounter == WORK_TICKS) { // Finish the job.
                        ItemStack inputSlot = this.getStackInSlot(SLOT_IN);
                        ItemStack outputSlot = this.getStackInSlot(SLOT_OUT);
                        inputSlot.stackSize -= current.input.stackSize;
                        if(inputSlot.stackSize == 0)
                            this.setInventorySlotContents(SLOT_IN, null);
                        
                        if(outputSlot != null)
                            outputSlot.stackSize += current.output.stackSize;
                        else
                            this.setInventorySlotContents(SLOT_OUT, current.output.copy());
                        
                        current = null;
                        workCounter = 0;
                    }
                } else {
                    current = null;
                    workCounter = 0;
                }
            } else {
                if(++workCounter == 5) {
                    current = MetalFormerRecipes.INSTANCE.getRecipe(this.getStackInSlot(SLOT_IN), mode);
                    workCounter = 0;
                }
            }
            
            /* Process energy in/out */ {
                ItemStack stack = this.getStackInSlot(SLOT_BATTERY);
                if(stack != null && EnergyItemHelper.isSupported(stack)) {
                    double gain = EnergyItemHelper
                            .pull(stack, Math.min(getMaxEnergy() - getEnergy(), getBandwidth()), false);
                    this.injectEnergy(gain);
                }
            }
            
            if(++updateCounter == 10) {
                updateCounter = 0;
                sync();
            }
        } else {
            updateSounds();
        }
    }
    
    // Cycle the mode. should be only called in SERVER.
    public void cycleMode(int delta) {
        int nextOrd = mode.ordinal() + delta;
        if (nextOrd >= Mode.values().length) nextOrd = 0;
        else if (nextOrd < 0) nextOrd = Mode.values().length - 1;

        mode = Mode.values()[nextOrd];
        sync();
    }
    
    // SERVER only
    private void sync() {
        NetworkMessage.sendToAllAround(
                TargetPoints.convert(this, 12),
                this, "sync",
                workCounter, current, mode
        );
    }
    
    private boolean isActionBlocked() {
        if(current == null) {
            return true;
        }
        
        ItemStack inputSlot = this.getStackInSlot(SLOT_IN), outputSlot = this.getStackInSlot(SLOT_OUT);
        return !(current.accepts(inputSlot, mode) && 
            (outputSlot == null || 
            (outputSlot.getItem() == current.output.getItem() && 
            outputSlot.getItemDamage() == current.output.getItemDamage() &&
            outputSlot.stackSize + current.output.stackSize <= outputSlot.getMaxStackSize())));
    }
    
    public boolean isWorkInProgress() {
        return current != null && !isActionBlocked();
    }
    
    public double getWorkProgress() {
        return isWorkInProgress() ? (double) workCounter / WORK_TICKS : 0;
    }

    @Listener(channel="sync", side=Side.CLIENT)
    private void syncData(int counter, @NullablePar RecipeObject recipe, Mode mode) {
        this.workCounter = counter;
        this.current = recipe;
        this.mode = mode;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        mode = Mode.values()[nbt.getInteger("mode")];
        super.readFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("mode", mode.ordinal());
        super.writeToNBT(nbt);
    }
    
    // --- CLIENT EFFECTS
    
    @SideOnly(Side.CLIENT)
    private PositionedSound sound;
    
    @SideOnly(Side.CLIENT)
    private void updateSounds() {
        if(sound != null && !isWorkInProgress()) {
            sound.stop();
            sound = null;
        } else if(sound == null && isWorkInProgress()) {
            sound = new TileEntitySound(this, "machine.machine_work")
                    .setLoop().setVolume(.6f);
            ACSounds.playClient(sound);
        }
    }

}
