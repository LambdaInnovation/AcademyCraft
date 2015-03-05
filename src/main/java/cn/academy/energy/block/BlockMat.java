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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.academy.energy.client.gui.GuiMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.template.block.BlockDirectionalMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Energy Matrix
 * @author WeathFolD
 */
@RegistrationClass
public class BlockMat extends BlockDirectionalMulti {

	public BlockMat() {
		super(Material.anvil);
		setBlockName("ac_grid");
		setBlockTextureName("academy:grid");
		setCreativeTab(AcademyCraft.cct);
		setLightLevel(2.0F);
		setHardness(2.7f);
		addSubBlock(0, 0, 1);
		addSubBlock(1, 0, 1);
		addSubBlock(1, 0, 0);
	}
	
    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer p, 
			int s, float tx, float ty, float tz) {
    	int[] origin = this.getOrigin(world, x, y, z, world.getBlockMetadata(x, y, z));
    	if(origin != null) {
    		matGui.openGuiContainer(p, world, origin[0], origin[1], origin[2]);
    		return true;
    	}
        return false;
    }
    
    @Override
    public void breakBlock(World world, int x, int y, int z, 
    		Block block, int meta) {
    	int[] ori = this.getOrigin(world, x, y, z, meta);
    	TileEntity te = world.getTileEntity(ori[0], ori[1], ori[2]);
    	if(te instanceof TileMatrix) {
    		((TileMatrix)te).onBreak();
    	}
    	super.breakBlock(world, x, y, z, block, meta);
    }

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileMatrix();
	}

	@Override
	public Vec3 getRenderOffset() {
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public Vec3 getOffsetRotated(int dir) {
		switch(dir) {
		case 2:
			return Vec3.createVectorHelper(-1, 0, -1);
		case 3:
			return Vec3.createVectorHelper(2, 0, 2);
		case 4:
			return Vec3.createVectorHelper(-1, 0, 2);
		default:
			return Vec3.createVectorHelper(2, 0, -1);
		}
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

}
