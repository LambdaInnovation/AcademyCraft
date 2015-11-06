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
package cn.academy.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.util.mc.StackUtils;

/**
 * BaseClass for typical block containers. will automatically try to open the container gui.
 * @author WeAthFolD
 */
public abstract class ACBlockContainer extends BlockContainer {
	
	final GuiHandlerBase guiHandler;

	public ACBlockContainer(String name, Material mat) {
		this(name, mat, null);
	}
	
	public ACBlockContainer(String name, Material mat, GuiHandlerBase _guiHandler) {
		super(mat);
		guiHandler = _guiHandler;
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_" + name);
		setBlockTextureName("academy:" + name);
	}
	
	protected IIcon ricon(IIconRegister ir, String name) {
    	return ir.registerIcon("academy:" + name);
    }

	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(guiHandler != null && !player.isSneaking()) {
        	if(!world.isRemote)
        		guiHandler.openGuiContainer(player, world, x, y, z);
            return true;
        }
        return false;
    }
	
	@Override
    public void breakBlock(World world, int x, int y, int z, Block block, int wtf) {
		if(!world.isRemote) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof IInventory) {
				StackUtils.dropItems(world, x, y, z, (IInventory) te);
			}
		}
		super.breakBlock(world, x, y, z, block, wtf);
    }

}
