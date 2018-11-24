package cn.academy.block.tileentity;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.block.block.ACFluids;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.PositionedSound;
import cn.academy.client.sound.TileEntitySound;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.academy.item.ItemMatterUnit;
import cn.academy.energy.IFConstants;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.StackUtils;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.*;

/**
 * @author WeAthFolD
 */
@RegTileEntity
public class TileImagFusor extends TileReceiverBase implements IFluidHandler, ISidedInventory {
    
    static final double WORK_SPEED = 1.0 / 120;
    static final double CONSUME_PER_TICK = 12;
    static final int SYNC_INTV = 5;

    // TODO
//    @RegTileEntity.Render
//    @SideOnly(Side.CLIENT)
//    public static RenderDynamicBlock renderer;
    
    static final int TANK_SIZE = 8000;
    static final int PER_UNIT = 1000;
    
    //Inventory id:
    public static final int
        SLOT_INPUT = 0,
        SLOT_OUTPUT = 1,
        SLOT_IMAG_INPUT = 2,
        SLOT_ENERGY_INPUT = 3,
        SLOT_IMAG_OUTPUT = 4;

    private final int[] slotsTop = {SLOT_INPUT, SLOT_IMAG_INPUT};
    private final int[] slotsBottom = {SLOT_OUTPUT, SLOT_IMAG_OUTPUT, SLOT_ENERGY_INPUT};
    private final int[] slotsOther = {SLOT_ENERGY_INPUT};
    
    protected FluidTank tank = new FluidTank(TANK_SIZE);
    
    private IFRecipe currentRecipe;
    private double workProgress;
    
    private int checkCooldown = 10, syncCooldown = SYNC_INTV;

    public TileImagFusor() {
        super("imag_fusor", 5, 2000, IFConstants.LATENCY_MK1);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if(resource.getFluid() != ACFluids.fluidImagProj) {
            return 0;
        }
        return tank.fill(resource, doFill);
    }
    
    public int getTankSize() {
        return tank.getCapacity();
    }
    
    public int getLiquidAmount() {
        return tank.getFluidAmount();
    }
    
    /**
     * As has discussed, this should perform a fake drain-energy effect when crafting, and restore to the real energy when not.
     * @return The energy for client-side display purpose.
     */
    public double getEnergyForDisplay() {
        return getEnergy();
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource.getFluid() != ACFluids.fluidImagProj)
            return null;
        return tank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    @SuppressWarnings("sideonly")
    public void update() {
        super.update();

        if (!isWorking()) {
            // Match the work in server
            if(!world.isRemote) {
                if(--checkCooldown <= 0) {
                    checkCooldown = 10;
                    if(!inventory[SLOT_INPUT].isEmpty()) {
                        IFRecipe recipe = ImagFusorRecipes.INSTANCE.getRecipe(inventory[SLOT_INPUT]);
                        if(recipe != null) {
                            startWorking(recipe);
                        }
                    }
                }

            }
        }
        
        if(isWorking()) {
            updateWork();
        }
        
        // Update liquid
        {
            ItemStack imagOutStack = inventory[SLOT_IMAG_OUTPUT];
            if(!inventory[SLOT_IMAG_INPUT].isEmpty() &&
               (!imagOutStack.isEmpty() || imagOutStack.getCount() < imagOutStack.getMaxStackSize()) &&
                getLiquidAmount() + PER_UNIT <= TANK_SIZE) {

                this.tank.fill(new FluidStack(ACFluids.fluidImagProj, PER_UNIT), true);

                inventory[SLOT_IMAG_INPUT].shrink(1);
                if(inventory[SLOT_IMAG_INPUT].getCount() == 0)
                    inventory[SLOT_IMAG_INPUT] = ItemStack.EMPTY;

                if (imagOutStack.isEmpty()) {
                    inventory[SLOT_IMAG_OUTPUT] = ACItems.matter_unit.create(ItemMatterUnit.MAT_NONE);
                } else {
                    imagOutStack.grow(1);
                }

            }
        }
        
        // Update energy
        if(inventory[SLOT_ENERGY_INPUT] != null) {
            double gain = EnergyItemHelper
                    .pull(inventory[SLOT_ENERGY_INPUT], Math.min(getMaxEnergy() - getEnergy(), getBandwidth()), false);
            this.injectEnergy(gain);
        }


        if (world.isRemote) {
            updateSounds();
        }
    }
    
    //---Work API
    /**
     * @return The working progress, or 0.0 if isn't crafting
     */
    public double getWorkProgress() {
        if(!world.isRemote)
            return isWorking() ? workProgress : 0.0;
        else
            return workProgress;
    }
    
    private void startWorking(IFRecipe recipe) {
        currentRecipe = recipe;
        workProgress = 0.0;
    }
    
    private void updateWork() {
        // Check the input stack, and abort if item isnt there
            // Also check whether the amount of Liquid is enough,
            // and whether the output of currentRecipe can be outputed into outputslot
            // Added by Shielian
        if(inventory[0].isEmpty() || currentRecipe.consumeType.getItem() != inventory[0].getItem()
                || this.pullEnergy(CONSUME_PER_TICK) != CONSUME_PER_TICK || this.getLiquidAmount() < currentRecipe.consumeLiquid
                || (!inventory[SLOT_OUTPUT].isEmpty() && inventory[SLOT_OUTPUT].getItem() != currentRecipe.output.getItem())
                ) {
            abortWorking();
            return;
        }
        
        if(!isActionBlocked()) {
            workProgress += WORK_SPEED;
            if(workProgress >= 1.0) {
                endWorking();
            }
        }
    }
    
    private void endWorking() {
        if(isWorking()) {
            int drained = tank.drain(currentRecipe.consumeLiquid, true).amount;
            if(!world.isRemote) {
                inventory[0].shrink(currentRecipe.consumeType.getCount());
                if(inventory[0].getCount() <= 0)
                    inventory[0]=ItemStack.EMPTY;
                
                if(!inventory[1].isEmpty()) {
                    inventory[1].grow(currentRecipe.output.getCount());
                } else {
                    inventory[1] = currentRecipe.output.copy();
                }
            }
        }
        
        workProgress = 0.0;
        currentRecipe = null;
        checkCooldown = 0; // Avoid work pausing
    }
    
    private void abortWorking() {
        workProgress = 0.0;
        currentRecipe = null ;
    }
    
    public boolean isWorking() {
        return currentRecipe != null;
    }
    
    public boolean isActionBlocked() {
        return !isWorking() || 
            (inventory[0].getCount() < currentRecipe.consumeType.getCount()) ||
            (!inventory[1].isEmpty() && (!StackUtils.isStackDataEqual(inventory[1], currentRecipe.output) ||
            inventory[1].getCount() + currentRecipe.output.getCount() > inventory[1].getMaxStackSize())) ||
            currentRecipe.consumeLiquid > this.getLiquidAmount();
    }
    
    public IFRecipe getCurrentRecipe() {
        return currentRecipe;
    }
    
    //---

    @Override
    public IFluidTankProperties[] getTankProperties() {
        FluidTankInfo info = tank.getInfo();
        return new IFluidTankProperties[] {
            new FluidTankProperties(info.fluid, info.capacity)
        };
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tank.readFromNBT(tag);
        int curRcp = tag.getInteger("_work_recipe");
        if(curRcp==-1)
            currentRecipe=null;
        else
            currentRecipe= ImagFusorRecipes.INSTANCE.getAllRecipe().get(curRcp);
        workProgress = tag.getDouble("_work_progress");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
        tag.setInteger("_work_recipe", currentRecipe==null?-1:currentRecipe.getID());
        tag.setDouble("_work_progress", workProgress);
        return tag;
    }

    
    // --- CLIENT EFFECTS
    
    @SideOnly(Side.CLIENT)
    private PositionedSound sound;
    
    @SideOnly(Side.CLIENT)
    private void updateSounds() {
        boolean play = !isActionBlocked();

        if(sound != null && !play) {
            sound.stop();
            sound = null;
        } else if(sound == null && play) {
            sound = new TileEntitySound(this, "machine.imag_fusor_work").setLoop().setVolume(0.6f);
            ACSounds.playClient(sound);
        }
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        switch(side) {
            case DOWN:
                return slotsBottom;
            case UP:
                return slotsTop;
            default:
                return slotsOther;
        }
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, EnumFacing facing) {
        return this.isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, EnumFacing facing) {
        return facing == EnumFacing.DOWN;
    }
}