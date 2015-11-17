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

import cn.academy.ability.develop.DeveloperType;
import cn.academy.core.block.ACBlockMulti;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.template.client.render.block.RenderEmptyBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class BlockDeveloper extends ACBlockMulti {
	
	public final DeveloperType type;

	public BlockDeveloper(DeveloperType _type) {
		super("developer", Material.rock);
		type = _type;
		setHardness(4.0f);
		setHarvestLevel("pickaxe", 2);
		
		String tmp = type.toString().toLowerCase();
		setBlockName("ac_developer_" + tmp);
		setBlockTextureName("academy:developer_" + tmp);
		
		this.addSubBlock(0, 1, 0);
		this.addSubBlock(0, 0, 1);
		this.addSubBlock(0, 1, 1);
		this.addSubBlock(0, 2, 1);
		this.addSubBlock(0, 0, 2);
		this.addSubBlock(0, 1, 2);
		this.addSubBlock(0, 2, 2);
		
		finishInit();
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
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(!world.isRemote && !player.isSneaking()) {
        	TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileDeveloper) {
				TileDeveloper td = (TileDeveloper) te;
				if(td.getUser() == null) {
					td.use(player);
				}
			}
            return true;
        }
        return false;
    }

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return type == DeveloperType.NORMAL ? new TileDeveloper.Normal() : new TileDeveloper.Advanced();
	}

	@Override
	public double[] getRotCenter() {
		return new double[] { 0.5, 0, 0.5 };
	}

}
