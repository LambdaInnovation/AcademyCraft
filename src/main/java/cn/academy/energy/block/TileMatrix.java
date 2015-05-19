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
import net.minecraft.util.AxisAlignedBB;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.api.IWirelessMatrix;
import cn.academy.energy.client.render.block.RenderMatrix;
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
public class TileMatrix extends TileInventory implements IWirelessMatrix, IMultiTile {

	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMatrix renderer;
	
	public TileMatrix() {
		super("wireless_matrix", 4);
	}
	
	//InfoBlockMulti delegation
	InfoBlockMulti info = new InfoBlockMulti(this);
	
	@Override
	public void updateEntity() {
		if(info != null)
			info.update();
	}

	@Override
	public InfoBlockMulti getBlockInfo() {
		return info;
	}

	@Override
	public void setBlockInfo(InfoBlockMulti i) {
		info = i;
	}

	@Override
    public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	info = new InfoBlockMulti(this, nbt);
    }
    
	@Override
    public void writeToNBT(NBTTagCompound nbt) {
    	super.writeToNBT(nbt);
    	info.save(nbt);
    }

	//WEN TODO
	@Override
	public int getCapacity() {
		return 10;
	}

	@Override
	public double getLatency() {
		return 100;
	}

	@Override
	public double getRange() {
		return 20;
	}
	
	//AABB
	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}
}
