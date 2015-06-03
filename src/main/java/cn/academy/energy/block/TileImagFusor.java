/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import cn.academy.core.block.TileReceiverBase;
import cn.academy.crafting.ModuleCrafting;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;

/**
 * TODO: Implement recipe system and server-side mechanism
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity	
public class TileImagFusor extends TileReceiverBase implements IFluidHandler {
	
	static final int TANK_SIZE = 8000;
	
	//Inventory id:
	// 0: Input
	// 1: Output
	// 2: Imag input
	// 3: Energy input
	
	protected FluidTank tank = new FluidTank(TANK_SIZE);

	public TileImagFusor() {
		super("imag_fusor", 4, 100000, 50);
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
		return 5000;
		//return tank.getFluidAmount();
	}
	
	/**
	 * @return The crafting progress, or 0.0 if isn't crafting
	 */
	public double getCraftProgress() {
		//TODO: Implement
		return 0.5;
	}
	
	/**
	 * As has discussed, this should perform a fake drain-energy effect when crafting, and restore to the real energy when not.
	 * @return The energy for client-side display purpose.
	 */
	public double getEnergyForDisplay() {
		//TODO: Implement
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
		return true;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return true;
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
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
    }

}
