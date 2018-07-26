package cn.academy.crafting.block;

import cn.academy.core.block.TileReceiverBase;
import cn.academy.core.client.render.block.RenderDynamicBlock;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.PositionedSound;
import cn.academy.core.client.sound.TileEntitySound;
import cn.academy.core.network.MessageMachineInfoSync;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.energy.IFConstants;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.annoreg.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.mc.StackUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

/**
 * @author WeAthFolD
 */
@RegTileEntity
@RegTileEntity.HasRender
public class TileImagFusor extends TileReceiverBase implements IFluidHandler, ISidedInventory {
    
    static final double WORK_SPEED = 1.0 / 120;
    static final double CONSUME_PER_TICK = 12;
    static final int SYNC_INTV = 5;
    
    @RegTileEntity.Render
    @SideOnly(Side.CLIENT)
    public static RenderDynamicBlock renderer;
    
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
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if(resource.getFluid() != ModuleCrafting.fluidImagProj) {
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
    public FluidStack drain(ForgeDirection from, FluidStack resource,
            boolean doDrain) {
        return tank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return fluid == ModuleCrafting.fluidImagProj;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return fluid == ModuleCrafting.fluidImagProj;
    }
    
    @Override
    public void update() {
        super.update();

        if (!isWorking()) {
            // Match the work in server
            if(!worldObj.isRemote) {
                if(--checkCooldown <= 0) {
                    checkCooldown = 10;
                    if(inventory[SLOT_INPUT] != null) {
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
            if(inventory[SLOT_IMAG_INPUT] != null &&
               (imagOutStack == null || imagOutStack.stackSize < imagOutStack.getMaxStackSize()) &&
                getLiquidAmount() + PER_UNIT <= TANK_SIZE) {

                this.tank.fill(new FluidStack(ModuleCrafting.fluidImagProj, PER_UNIT), true);

                inventory[SLOT_IMAG_INPUT].stackSize--;
                if(inventory[SLOT_IMAG_INPUT].stackSize == 0)
                    inventory[SLOT_IMAG_INPUT] = null;

                if (imagOutStack == null) {
                    inventory[SLOT_IMAG_OUTPUT] = ModuleCrafting.matterUnit.create(ItemMatterUnit.NONE);
                } else {
                    ++imagOutStack.stackSize;
                }

            }
        }
        
        // Update energy
        if(inventory[SLOT_ENERGY_INPUT] != null) {
            double gain = EnergyItemHelper
                    .pull(inventory[SLOT_ENERGY_INPUT], Math.min(getMaxEnergy() - getEnergy(), getBandwidth()), false);
            this.injectEnergy(gain);
        }
        
        // Synchronization
        if (!worldObj.isRemote) {
            if(--syncCooldown <= 0) {
                syncCooldown = SYNC_INTV;
                cn.academy.core.network.NetworkManager.instance.sendToAllAround(new MessageMachineInfoSync(this), TargetPoints.convert(this, 15));
            }
        }

        if (worldObj.isRemote) {
            updateSounds();
        }
    }
    
    //---Work API
    /**
     * @return The working progress, or 0.0 if isn't crafting
     */
    public double getWorkProgress() {
        return isWorking() ? workProgress : 0.0;
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
        if(inventory[0] == null || currentRecipe.consumeType.getItem() != inventory[0].getItem()
                || this.pullEnergy(CONSUME_PER_TICK) != CONSUME_PER_TICK || this.getLiquidAmount() < currentRecipe.consumeLiquid
                || (inventory[SLOT_OUTPUT] != null && inventory[SLOT_OUTPUT].getItem() != currentRecipe.output.getItem())
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
            if(!worldObj.isRemote) {
                inventory[0].stackSize -= currentRecipe.consumeType.stackSize;
                if(inventory[0].stackSize <= 0)
                    inventory[0] = null;
                
                if(inventory[1] != null) {
                    inventory[1].stackSize += currentRecipe.output.stackSize;
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
            (inventory[0].stackSize < currentRecipe.consumeType.stackSize) ||
            (inventory[1] != null && (!StackUtils.isStackDataEqual(inventory[1], currentRecipe.output) || 
            inventory[1].stackSize + currentRecipe.output.stackSize > inventory[1].getMaxStackSize())) ||
            currentRecipe.consumeLiquid > this.getLiquidAmount();
    }
    
    public IFRecipe getCurrentRecipe() {
        return currentRecipe;
    }
    
    //---

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] { tank.getInfo() };
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tank.readFromNBT(tag);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
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
    public int[] getAccessibleSlotsFromSide(int side) {
        switch(side) {
            case 0:
                return slotsBottom;
            case 1:
                return slotsTop;
            default:
                return slotsOther;
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
}