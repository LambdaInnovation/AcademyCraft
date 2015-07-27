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
package cn.academy.ability.block;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.core.block.ACBlockContainer;
import cn.liutils.template.client.render.block.RenderEmptyBlock;

/**
 * @author WeAthFolD
 *
 */
public class BlockDeveloper extends ACBlockContainer {
	
	public final DeveloperType type;

	public BlockDeveloper(DeveloperType _type) {
		super("developer", Material.rock, null);
		type = _type;
	}
	
	@Override
	public int getRenderType() {
		return RenderEmptyBlock.id;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileDeveloper();
	}

}
