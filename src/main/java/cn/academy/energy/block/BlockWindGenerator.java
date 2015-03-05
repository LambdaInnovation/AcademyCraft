/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.TileWindGenerator;
import cn.liutils.template.block.BlockDirectionalMulti;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;


/**
 * Wind Generator
 * TODO: Not added in β
 * @author WeAthFolD
 */
public class BlockWindGenerator extends BlockDirectionalMulti {

	public BlockWindGenerator() {
		super(Material.rock);
		setBlockName("ac_windgen");
		setBlockTextureName("academy:windgen");
		setCreativeTab(AcademyCraft.cct);
		setHardness(2.0f);
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileWindGenerator();
	}

	@Override
	public Vec3 getRenderOffset() {
		return null;
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public Vec3 getOffsetRotated(int dir) {
    	return Vec3.createVectorHelper(0.5D, 0D, 0.5D);
    }


}
