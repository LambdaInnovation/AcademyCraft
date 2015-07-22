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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.ModuleEnergy;
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
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderWindGenBase renderer;

	public TileWindGenBase() {
		super("windgen_base", 1, 1000, 100);
	}

	@Override
	public double getGeneration(double required) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// InfoBlockMulti delegates
	InfoBlockMulti info = new InfoBlockMulti(this);
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		info.update();
	}
	
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		info = new InfoBlockMulti(this, tag);
	}
	
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
		int state = 1;
		int pillars = 0;
		
		for(int y = yCoord + 1; state < 2; ++y) {
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
						state = 2;
					} else {
						break;
					}
				} else {
					state = 3;
					break;
				}
			}
		}
		
		return state == 2 && pillars >= WindGenerator.MIN_PILLARS;
	}
}
