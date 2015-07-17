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

import net.minecraft.nbt.NBTTagCompound;
import cn.academy.core.tile.TileInventory;
import cn.academy.energy.client.render.block.RenderWindGenMain;
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
public class TileWindGenMain extends TileInventory implements IMultiTile {
	
	// State for render
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderWindGenMain renderer;
	
	@SideOnly(Side.CLIENT)
	public long lastFrame = -1;
	@SideOnly(Side.CLIENT)
	public float lastRotation;
	
	public TileWindGenMain() {
		super("windgen_main", 1);
	}

	// Spin logic
	// TODO Implement
	public boolean isFanInstalled() {
		return true;
	}
	
	/**
	 * Unit: Degree per second
	 */
	public double getSpinSpeed() {
		return 60.0;
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
	
}
