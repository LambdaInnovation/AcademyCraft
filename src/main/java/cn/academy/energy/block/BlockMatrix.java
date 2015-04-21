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

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.liutils.template.block.BlockMulti;

/**
 * @author WeAthFolD
 *
 */
public class BlockMatrix extends BlockMulti {
	
	public enum MatrixType {
		BASIC(5, 10, 10), STANDARD(10, 20, 100), ADVANCED(20, 30, 1000);
		MatrixType(int cap, double r, double lat) {
			capacity = cap;
			range = r;
			latency = lat;
		}
		
		public final double latency, range;
		public final int capacity;
	};

	public final MatrixType type;
	
	public BlockMatrix(MatrixType mt) {
		super(Material.rock);
		type = mt;
		
		setCreativeTab(AcademyCraft.cct);
		setBlockName("matrix_" + mt.toString().toLowerCase());
		setBlockTextureName("academy:matrix_" + mt.toString().toLowerCase());
		setHardness(3.0f);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileMatrix();
	}

	@Override
	public double[] getRotCenter() {
		return new double[] { 0, 0, 0};
	}
	
	@Override
    public int getRenderType() {
        return 0;
    }

}
