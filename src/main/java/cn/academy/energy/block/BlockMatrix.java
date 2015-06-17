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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.client.gui.matrix.GuiMatrix;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.template.block.BlockMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class BlockMatrix extends BlockMulti {
	
	public BlockMatrix() {
		super(Material.rock);
		setCreativeTab(AcademyCraft.cct);
		setBlockName("ac_matrix");
		setBlockTextureName("academy:matrix");
		setHardness(3.0f);
		setLightLevel(1f);
		
		addSubBlock(0, 0, 1);
		addSubBlock(1, 0, 1);
		addSubBlock(1, 0, 0);
		
		addSubBlock(0, 1, 0);
		addSubBlock(0, 1, 1);
		addSubBlock(1, 1, 1);
		addSubBlock(1, 1, 0);
		
		this.finishInit();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileMatrix();
	}

	@Override
	public double[] getRotCenter() {
		return new double[] { 1.0, 0, 1.0};
	}
	
//	@Override
//    public int getRenderType() {
//        return 0;
//    }
	
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(!player.isSneaking()) {
        	int[] center = this.getOrigin(world, x, y, z);
        	if(center != null) {
        		guiHandler.openGuiContainer(player, world, center[0], center[1], center[2]);
        	}
            return true;
        }
        return false;
    }
	
	@RegGuiHandler
	public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		@Override
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileMatrix te = check(world, x, y, z);
			return te == null ? null : new GuiMatrix(new ContainerMatrix(te, player));
		}
		
		@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileMatrix te = check(world, x, y, z);
			return te == null ? null : new ContainerMatrix(te, player);
		}
		
		private TileMatrix check(World world, int x, int y, int z) {
			TileEntity te = world.getTileEntity(x, y, z);
			return (TileMatrix) (te instanceof TileMatrix ? te : null);
		}
	};

}
