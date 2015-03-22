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
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.academy.energy.client.gui.GuiMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.template.block.BlockMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Energy Matrix
 * @author WeathFolD
 */
@RegistrationClass
public class BlockMat extends BlockMulti {

	public BlockMat() {
		super(Material.anvil);
		setBlockName("ac_grid");
		setBlockTextureName("academy:grid");
		setCreativeTab(AcademyCraft.cct);
		setLightLevel(2.0F);
		setHardness(2.7f);
	}
	
    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer p, 
			int s, float tx, float ty, float tz) {
    	int[] origin = this.getOrigin(world, x, y, z);
    	if(origin != null) {
    		matGui.openGuiContainer(p, world, origin[0], origin[1], origin[2]);
    		return true;
    	}
        return false;
    }

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileMatrix();
	}
	
	@RegGuiHandler
	public static GuiHandlerBase matGui = new GuiHandlerBase() {
		@Override
		@SideOnly(Side.CLIENT)
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileEntity te = world.getTileEntity(x, y, z);
			return te instanceof TileMatrix ? new GuiMatrix((TileMatrix) te) : null;
		}
		
		@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			return null;
		}
	};

	@Override
	public void initSubBlock() {
		addSubBlock(0, 0, 1);
		addSubBlock(1, 0, 1);
		addSubBlock(1, 0, 0);
		addSubBlock(0, 1, 0);
		addSubBlock(0, 1, 1);
		addSubBlock(1, 1, 1);
		addSubBlock(1, 1, 0);
	}

	@Override
	public double[] getRotCenter() {
		return new double[] { 1, 0, 1 };
	}

}
