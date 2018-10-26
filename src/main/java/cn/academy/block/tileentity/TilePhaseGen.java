package cn.academy.block.tileentity;

import cn.academy.ACItems;
import cn.academy.block.block.ACFluids;
import cn.academy.block.container.ContainerPhaseGen;
import cn.academy.item.ItemMatterUnit;
import cn.academy.energy.IFConstants;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.TargetPoints;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@RegTileEntity
public class TilePhaseGen extends TileGeneratorBase implements IFluidHandler {
    
    // 废话~~~
    public static final int
        SLOT_LIQUID_IN = ContainerPhaseGen.SLOT_LIQUID_IN,
        SLOT_LIQUID_OUT = ContainerPhaseGen.SLOT_LIQUID_OUT,
        SLOT_OUTPUT = ContainerPhaseGen.SLOT_OUTPUT;

    // TODO
//    @RegTileEntity.Render
//    @SideOnly(Side.CLIENT)
//    public static RenderPhaseGen renderer;
//
    static final int CONSUME_PER_TICK = 100;
    static final double GEN_PER_MB = 0.5;
    
    int untilSync;

    public TilePhaseGen() {
        super("phase_gen", 3, 6000, IFConstants.LATENCY_MK1);
    }

    @Override
    public double getGeneration(double required) {
        int maxDrain = (int) Math.min(CONSUME_PER_TICK, required / GEN_PER_MB);
        FluidStack fs = tank.drain(maxDrain, true);
        return fs == null ? 0 : fs.amount * GEN_PER_MB;
    }

    @Override
    public void update() {
        super.update();
        
        if(!getWorld().isRemote) {
            if(++untilSync == 10) {
                untilSync = 0;
                sync();
            }
            
            ItemStack stack;
            { // Sink in liquid
                stack = getStackInSlot(SLOT_LIQUID_IN);
                
                if(!stack.isEmpty() && isPhaseLiquid(stack) && isOutputSlotAvailable() &&
                        (getTankSize() - getLiquidAmount() > PER_UNIT)) {
                    
                    if(stack.getCount() > 0) {
                        tank.fill(new FluidStack(ACFluids.fluidImagProj, PER_UNIT), true);
                        stack.shrink(1);
                    }
                    
                    ItemStack output = getStackInSlot(SLOT_LIQUID_OUT);
                    if(!output.isEmpty()) {
                        output.grow(1);
                    } else {
                        this.setInventorySlotContents(SLOT_LIQUID_OUT,
                            ACItems.matter_unit.create(ItemMatterUnit.MAT_NONE));
                    }
                }
            }
            
            { // Output energy
                stack = getStackInSlot(SLOT_OUTPUT);
                if(!stack.isEmpty())
                    this.tryChargeStack(stack);
            }
        }
    }
    
    // Fluid handling
    static final int TANK_SIZE = 8000;
    static final int PER_UNIT = 1000;
    
    protected FluidTank tank = new FluidTank(TANK_SIZE);
    
    public int getLiquidAmount() {
        return tank.getFluidAmount();
    }
    
    public int getTankSize() {
        return tank.getCapacity();
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        FluidTankInfo info = tank.getInfo();
        return new IFluidTankProperties[] {
            new FluidTankProperties(info.fluid, info.capacity)
        };
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if(resource.getFluid() != ACFluids.fluidImagProj) {
            return 0;
        }
        return tank.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(FluidStack resource,
            boolean doDrain) {
        if (resource.getFluid() != ACFluids.fluidImagProj)
            return null;
        return tank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
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

    private void sync() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, "MSG_INFO_SYNC", nbt);
    }

    @NetworkMessage.Listener(channel="MSG_INFO_SYNC", side= Side.CLIENT)
    private void onSync(NBTTagCompound nbt)
    {
        readFromNBT(nbt);
    }

    private boolean isPhaseLiquid(ItemStack stack) {
        return stack.getItem() == ACItems.matter_unit &&
                ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_PHASE_LIQUID;
    }
    
    private boolean isOutputSlotAvailable() {
        ItemStack stack = getStackInSlot(SLOT_LIQUID_OUT);
        return stack.isEmpty() || (stack.getItem() == ACItems.matter_unit &&
                ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_NONE && stack.getCount() < stack.getMaxStackSize());
    }

}