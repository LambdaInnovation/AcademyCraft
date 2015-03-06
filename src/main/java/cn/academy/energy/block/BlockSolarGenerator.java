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

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.TileSolarGenerator;
import cn.liutils.core.proxy.LIClientProps;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;


/**
 * Solar Generator
 * @author WeAthFolD
 *
 */
public class BlockSolarGenerator extends Block implements ITileEntityProvider {

	public BlockSolarGenerator() {
		super(Material.iron);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_solar");
		setBlockTextureName("academy:solar");
		setHardness(2.0F);
	}
	
	@Override
    public boolean isOpaqueCube() {
		return false;
    }
	
	@Override
    public boolean renderAsNormalBlock() {
        return false;
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderType() {
		return LIClientProps.RENDER_TYPE_EMPTY;
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileSolarGenerator();
	}


}
