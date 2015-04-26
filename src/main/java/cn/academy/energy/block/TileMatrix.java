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

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import cn.academy.energy.api.IWirelessMatrix;
import cn.academy.energy.block.BlockMatrix.MatrixType;
import cn.academy.energy.client.render.block.RenderMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.block.TileMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileMatrix extends TileMulti implements IWirelessMatrix {
	
	@RegTileEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMatrix renderer;

	@Override
	public int getCapacity() {
		return getMatrixType().capacity;
	}

	@Override
	public double getLatency() {
		return getMatrixType().latency;
	}

	@Override
	public double getRange() {
		return getMatrixType().range;
	}
	
	private MatrixType getMatrixType() {
		Block bt = getBlockType();
		return bt instanceof BlockMatrix ? ((BlockMatrix)bt).type : MatrixType.STANDARD;
	}

	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}
}
