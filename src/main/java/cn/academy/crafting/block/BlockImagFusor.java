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
package cn.academy.crafting.block;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cn.academy.core.block.ACBlockContainer;
import cn.academy.crafting.client.gui.GuiImagFusor;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cn.lambdalib.util.helper.GameTimer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class BlockImagFusor extends ACBlockContainer {
	
	IIcon bottom, top, mside, side_idle;
	IIcon[] side_working = new IIcon[4];

	public BlockImagFusor() {
		super("imag_fusor", Material.rock, guiHandler);
		setHardness(3.0f);
		setHarvestLevel("pickaxe", 1);
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
    	bottom = ricon(ir, "machine_bottom");
    	top = ricon(ir, "machine_top");
    	mside = ricon(ir, "machine_side");
    	side_idle = ricon(ir, "ief_off");
    	
    	for(int i = 0; i < 4; ++i) {
    		side_working[i] = ricon(ir, "ief_working_" + (i + 1));
    	}
    }
    
    @Override
	@SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
    	TileImagFusor te = check(world, x, y, z);
    	
    	boolean working = false;
    	if(te != null) {
    		working = te.isWorking();
    	}
    	
    	return getIcon(world.getBlockMetadata(x, y, z) & 3, side, working);
    }
    
    @Override
    public IIcon getIcon(int side, int meta) {
    	return getIcon(meta & 3, side, false);
    }
    
	@Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
    	int l = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
    	l = (l + 2) % 4;
    	world.setBlockMetadataWithNotify(x, y, z, l, 0x03);
	}
	
	static final int[] map = { 2, 0, 1, 3 };
    
    private IIcon getIcon(int dir, int side, boolean working) {
    	switch(side) {
    	case 0:
    		return bottom;
    	case 1:
    		return top;
    	case 2:
    	case 3:
    	case 4:
    	case 5:
    		if(dir != (map[side - 2])) return this.mside;
    		if(!working) return side_idle;
    		return side_working[(int) ((GameTimer.getTime() / 400) % 4)];
    	default:
    		throw new RuntimeException("WTF");
    	}
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderBlockPass() {
        return -1;
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileImagFusor();
	}
	
	@Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
    	TileImagFusor te = check(world, x, y, z);
    	if(te == null)
    		return super.getLightValue(world, x, y, z);
    	return te.isWorking() ? 6 : 0;
    }
	
	@RegGuiHandler
	public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		@Override
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileImagFusor te = check(world, x, y, z);
			return te == null ? null : new GuiImagFusor(new ContainerImagFusor(te, player));
		}
		
		@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileImagFusor te = check(world, x, y, z);
			return te == null ? null : new ContainerImagFusor(te, player);
		}
		
	};
	
	private static TileImagFusor check(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		return (TileImagFusor) (te instanceof TileImagFusor ? te : null);
	}

}
