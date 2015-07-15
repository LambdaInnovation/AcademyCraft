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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import cn.academy.core.block.TileGeneratorBase;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.client.render.block.RenderPhaseGen;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TilePhaseGen extends TileGeneratorBase implements IFluidHandler {
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderPhaseGen renderer;
	
	static final int CONSUME_PER_TICK = 4;
	static final double GEN_PER_MB = 2;

	public TilePhaseGen() {
		super("phase_gen", 0, 1000, 100);
	}

	@Override
	public double getGeneration(double required) {
		int maxDrain = (int) Math.min(CONSUME_PER_TICK, required / GEN_PER_MB);
		FluidStack fs = tank.drain(maxDrain, true);
		return fs == null ? 0 : fs.amount * GEN_PER_MB;
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
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
    }

}
