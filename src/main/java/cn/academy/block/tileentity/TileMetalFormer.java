package cn.academy.block.tileentity;

import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.PositionedSound;
import cn.academy.client.sound.TileEntitySound;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.crafting.MetalFormerRecipes.RecipeObject;
import cn.academy.energy.IFConstants;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkMessage.NullablePar;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@RegTileEntity
public class TileMetalFormer extends TileReceiverBase implements ISidedInventory {

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        switch(side) {
            case DOWN:
                return new int[]{SLOT_OUT, SLOT_BATTERY};
            case UP:
                return new int[]{SLOT_IN};
            default:
                return new int[]{SLOT_BATTERY};
        }
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return isItemValidForSlot(index, itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return direction.getIndex() == 0;
    }

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
    @SuppressWarnings("sideonly")
    public void update() {
        super.update();
        
        World world = getWorld();
        if(!world.isRemote) {
            if(current != null) {
                // Process recipe
                if(!isActionBlocked() && this.pullEnergy(CONSUME_PER_TICK) == CONSUME_PER_TICK) {
                    ++workCounter;
                    if(workCounter == WORK_TICKS) { // Finish the job.
                        ItemStack inputSlot = this.getStackInSlot(SLOT_IN);
                        ItemStack outputSlot = this.getStackInSlot(SLOT_OUT);
                        inputSlot.shrink(current.input.getCount());
                        if(inputSlot.getCount() == 0)
                            this.setInventorySlotContents(SLOT_IN, ItemStack.EMPTY);
                        
                        if(!outputSlot.isEmpty())
                            outputSlot.grow(current.output.getCount());
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
                if(!stack.isEmpty() && EnergyItemHelper.isSupported(stack)) {
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
            (outputSlot.isEmpty() ||
            (outputSlot.getItem() == current.output.getItem() && 
            outputSlot.getItemDamage() == current.output.getItemDamage() &&
            outputSlot.getCount() + current.output.getCount() <= outputSlot.getMaxStackSize())));
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
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("mode", mode.ordinal());
        return super.writeToNBT(nbt);
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