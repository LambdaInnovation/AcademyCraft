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
package cn.academy.crafting.block;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import cn.academy.core.block.TileReceiverBase;
import cn.academy.core.client.render.block.RenderDynamicBlock;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.academy.support.EnergyItemHelper;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cn.liutils.util.mc.StackUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileImagFusor extends TileReceiverBase implements IFluidHandler {
	
	static final double WORK_SPEED = 0.03;
	static final int SYNC_INTV = 5;
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderDynamicBlock renderer;
	
	static final int TANK_SIZE = 8000;
	static final int PER_UNIT = 1000;
	
	//Inventory id:
	// 0: Input
	// 1: Output
	// 2: Imag input
	// 3: Energy input
	
	protected FluidTank tank = new FluidTank(TANK_SIZE);
	
	private IFRecipe currentRecipe;
	private double workProgress;
	
	private int checkCooldown = 10, syncCooldown = SYNC_INTV;

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
	public void updateEntity() {
		super.updateEntity();
		
		if(isWorking()) {
			updateWork();
		} else {
			// Match the work in server
			if(!worldObj.isRemote) {
				
				if(--checkCooldown == 0) {
					checkCooldown = 10;
					if(inventory[0] != null) {
						IFRecipe recipe = 
							ImagFusorRecipes.INSTANCE.getRecipe(inventory[0].getItem());
						if(recipe != null) {
							startWorking(recipe);
						}
					}
				}
				
			}
			
		}
		
		// Update liquid
		if(inventory[2] != null) {
			if(getLiquidAmount() + PER_UNIT <= TANK_SIZE) {
				this.tank.fill(new FluidStack(ModuleCrafting.fluidImagProj, PER_UNIT), true);
				inventory[2].stackSize--;
				if(inventory[2].stackSize == 0)
					inventory[2] = null;
			}
		}
		
		// Update energy
		if(inventory[3] != null) {
			double gain = EnergyItemHelper
					.pull(inventory[3], Math.min(getMaxEnergy() - getEnergy(), getBandwidth()), false);
			this.injectEnergy(gain);
		}
		
		// Synchronization
		if(worldObj.isRemote) {
			syncClient();
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void syncClient() {
		if(--syncCooldown == 0) {
			syncCooldown = SYNC_INTV;
			query(Minecraft.getMinecraft().thePlayer);
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
		if(currentRecipe.consumeType.getItem() != inventory[0].getItem()) {
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
			inventory[1].stackSize + currentRecipe.output.stackSize < inventory[1].getMaxStackSize())) ||
			currentRecipe.consumeLiquid > this.getLiquidAmount();
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
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
    }
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void query(@Instance EntityPlayer player) {
    	if(player != null)
    		syncBack(player, currentRecipe, workProgress, tank.getFluidAmount());
    }
    
    @RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
    private void syncBack(@Target EntityPlayer player, @Instance IFRecipe recipe, 
    	@Data Double progress, @Data Integer fluidAmount) {
    	tank.setFluid(new FluidStack(ModuleCrafting.fluidImagProj, fluidAmount));
    	workProgress = progress;
    	currentRecipe = recipe;
    }

}
