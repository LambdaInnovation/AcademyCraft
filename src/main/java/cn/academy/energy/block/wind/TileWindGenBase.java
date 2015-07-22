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
package cn.academy.energy.block.wind;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.client.render.block.RenderWindGenBase;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.block.IMultiTile;
import cn.liutils.template.block.InfoBlockMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileWindGenBase extends TileGeneratorBase implements IMultiTile {
	
	public static double MAX_GENERATION_SPEED = 100;
	
	private static final IFItemManager itemManager = IFItemManager.instance;
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderWindGenBase renderer;
	
	// CLIENT STATES
	public TileWindGenMain mainTile;
	public boolean complete;
	public int untilUpdate;
	
	public TileWindGenBase() {
		super("windgen_base", 1, 2000, 100);
	}

	@Override
	public double getGeneration(double required) {
		double sim = getSimulatedGeneration();
		return Math.min(required, sim);
	}
	
	// TODO: Nail down formula
	public double getSimulatedGeneration() {
		if(complete) {
			return 50.0;
		} else {
			return 0.0;
		}
	}
	
    private void updateChargeOut() {
    	ItemStack stack = this.getStackInSlot(0);
    	if(stack != null && itemManager.isSupported(stack)) {
    		double cur = getEnergy();
    		if(cur > 0) {
    			cur = Math.min(getBandwidth(), cur);
    			double left = itemManager.charge(stack, cur);
    			
    			this.setEnergy(getEnergy() - (cur - left));
    		}
    	}
    }
	
	// InfoBlockMulti delegates
	InfoBlockMulti info = new InfoBlockMulti(this);
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		info.update();
		
		if(++untilUpdate == 10) {
			untilUpdate = 0;
			mainTile = findMainTile();
			complete = mainTile != null;
		}
		
		updateChargeOut();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		info = new InfoBlockMulti(this, tag);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		info.save(tag);
	}

	@Override
	public InfoBlockMulti getBlockInfo() {
		return info;
	}

	@Override
	public void setBlockInfo(InfoBlockMulti i) {
		info = i;
	}

	public boolean isCompleteStructure() {
		return findMainTile() != null;
	}
	
	private TileWindGenMain findMainTile() {
		int state = 1;
		int pillars = 0;
		
		for(int y = yCoord + 2; state < 2; ++y) {
			TileEntity te = worldObj.getTileEntity(xCoord, y, zCoord);
			Block block = worldObj.getBlock(xCoord, y, zCoord);
			if(state == 1) {
				if(block == ModuleEnergy.windgenPillar) {
					++pillars;
					if(pillars > WindGenerator.MAX_PILLARS)
						break;
				} else if(te instanceof TileWindGenMain) {
					TileWindGenMain gen = (TileWindGenMain) te;
					if(gen.getBlockInfo().getSubID() == 0) {
						return pillars >= WindGenerator.MIN_PILLARS ? gen : null;
					} else {
						break;
					}
				} else {
					state = 3;
					break;
				}
			}
		}
		
		return null;
	}
}
