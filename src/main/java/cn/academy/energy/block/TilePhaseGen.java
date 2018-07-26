/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.TileGeneratorBase;
import cn.academy.core.network.MessageMachineInfoSync;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.energy.IFConstants;
import cn.academy.energy.client.render.block.RenderPhaseGen;
import cn.lambdalib2.annoreg.core.Registrant;
import cn.lambdalib2.annoreg.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TilePhaseGen extends TileGeneratorBase implements IFluidHandler {
    
    // 废话~~~
    public static final int
        SLOT_LIQUID_IN = ContainerPhaseGen.SLOT_LIQUID_IN,
        SLOT_LIQUID_OUT = ContainerPhaseGen.SLOT_LIQUID_OUT,
        SLOT_OUTPUT = ContainerPhaseGen.SLOT_OUTPUT;
    
    @RegTileEntity.Render
    @SideOnly(Side.CLIENT)
    public static RenderPhaseGen renderer;
    
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
        
        if(!getWorldObj().isRemote) {
            if(++untilSync == 10) {
                untilSync = 0;
                sync();
            }
            
            ItemStack stack;
            { // Sink in liquid
                stack = getStackInSlot(SLOT_LIQUID_IN);
                
                if(stack != null && isPhaseLiquid(stack) && isOutputSlotAvailable() && 
                        (getTankSize() - getLiquidAmount() > PER_UNIT)) {
                    
                    if(stack.stackSize > 0) {
                        tank.fill(new FluidStack(ModuleCrafting.fluidImagProj, PER_UNIT), true);
                        --stack.stackSize;
                    }
                    if(stack.stackSize <= 0)
                        setInventorySlotContents(0, null);
                    
                    ItemStack output = getStackInSlot(SLOT_LIQUID_OUT);
                    if(output != null) {
                        ++output.stackSize;
                    } else {
                        this.setInventorySlotContents(SLOT_LIQUID_OUT, 
                            ModuleCrafting.matterUnit.create(ItemMatterUnit.NONE));
                    }
                }
            }
            
            { // Output energy
                stack = getStackInSlot(SLOT_OUTPUT);
                if(stack != null)
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
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if(resource.getFluid() != ModuleCrafting.fluidImagProj) {
            return 0;
        }
        return tank.fill(resource, doFill);
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

    private void sync() {
        cn.academy.core.network.NetworkManager.instance.sendToAllAround(new MessageMachineInfoSync(this), TargetPoints.convert(this, 15));

    }

    private boolean isPhaseLiquid(ItemStack stack) {
        return stack.getItem() == ModuleCrafting.matterUnit && 
                ModuleCrafting.matterUnit.getMaterial(stack) == ModuleCrafting.imagPhase.mat;
    }
    
    private boolean isOutputSlotAvailable() {
        ItemStack stack = getStackInSlot(SLOT_LIQUID_OUT);
        return stack == null || (stack.getItem() == ModuleCrafting.matterUnit && 
                ModuleCrafting.matterUnit.getMaterial(stack) == ItemMatterUnit.NONE && stack.stackSize < stack.getMaxStackSize());
    }

}
