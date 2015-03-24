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
package cn.academy.core.block.dev;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderMagInducer;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.liutils.template.block.BlockMulti;
import cn.liutils.template.block.TileMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The magnet inducer component of Ability Developer. increases its sync rate (and thus have higher stim successful probability)
 * @author WeathFolD
 */
@RegistrationClass
public class BlockMagInducer extends BlockMulti {
	
	@RegTileEntity
	@RegTileEntity.HasRender
	public static class Tile extends TileMulti {
		
		@RegTileEntity.Render
		@SideOnly(Side.CLIENT)
		public static RenderMagInducer render;
		
	}

	public BlockMagInducer() {
		super(Material.iron);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_magindc");
		setBlockTextureName("academy:mag");
		setHardness(3.0f);
		setBlockBounds(0, 0, 0, 1, 0.6f, 1);
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new Tile();
	}

	@Override
	public void initSubBlock() {}

	@Override
	@SideOnly(Side.CLIENT)
	public double[] getRotCenter() {
		return new double[] { .5, 0, .5 };
	}

}
