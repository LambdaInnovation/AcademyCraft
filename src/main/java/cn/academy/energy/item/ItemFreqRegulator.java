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
package cn.academy.energy.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.academy.energy.client.gui.GuiFreqRegulator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.template.block.BlockDirectionalMulti;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


/**
 * Frequency regulator
 * @author WeathFolD
 */
@RegistrationClass
public class ItemFreqRegulator extends Item {
	
	public static final int LIST_MAX = 8;
	
	public ItemFreqRegulator() {
		setMaxDamage(30);
		setUnlocalizedName("ac_freqreg");
		setTextureName("academy:freqreg");
		setCreativeTab(AcademyCraft.cct);
	}
	
    @Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, 
    		int x, int y, int z, int side, float tx, float ty, float tz) {
    	//TODO: High coupling code, consider better approach
    	Block block = world.getBlock(x, y, z);
    	//If this is a BlockDirectionalMulti, fall back to its origin block
    	if(block instanceof BlockDirectionalMulti) {
    		int[] coords = ((BlockDirectionalMulti)block).getOrigin(world, x, y, z, world.getBlockMetadata(x, y, z));
    		if(coords != null) {
    			x = coords[0];
    			y = coords[1];
    			z = coords[2];
    		} else {
    			return false;
    		}
    	}
    	
    	TileEntity te = world.getTileEntity(x, y, z);
    	if(!(te instanceof TileUserBase)) {
    		return false;
    	}
    	guiHandler.openGuiContainer(player, world, x, y, z);
    	stack.damageItem(1, player);
        return true;
    }
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
    	@Override
		@SideOnly(Side.CLIENT)
    	protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
    		TileUserBase te = tsGet(world, x, y, z);
    		if(te == null) return null;
    		return new GuiFreqRegulator(te);
    	}
    	
    	@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
    		return null;
    	}
    	
    	private TileUserBase tsGet(World world, int x, int y, int z) {
    		TileEntity te = world.getTileEntity(x, y, z);
    		if(!(te instanceof TileUserBase)) {
    			AcademyCraft.log.error("WTF? Not energy tile?");
    			return null;
    		}
    		return (TileUserBase) te;
    	}
    };
}
