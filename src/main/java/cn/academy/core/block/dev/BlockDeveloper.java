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

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.register.ACItems;
import cn.liutils.template.block.BlockMulti;

/**
 * @author WeathFolD
 */
public class BlockDeveloper extends BlockMulti {

	public BlockDeveloper() {
		super(Material.anvil);
		setBlockName("ac_developer");
		setBlockTextureName("academy:bed");
		setCreativeTab(AcademyCraft.cct);
		setHardness(4.0f);
		this.setBlockBounds(0, 0, 0, 1, 0.5F, 1);
	}
	
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float px, float py, float pz) {
		ItemStack stack = player.getCurrentEquippedItem();
		if(stack != null && stack.getItem() == ACItems.freqReg) {
			return false;
		}
		
		{ //Transform to head block
			int meta = world.getBlockMetadata(x, y, z);
			int[] coords = this.getOrigin(world, x, y, z);
			x = coords[0];
			y = coords[1];
			z = coords[2];
		}
		
		TileDeveloper te = safecast(world.getTileEntity(x, y, z));
		if(te == null) return false;
		EntityPlayer user = te.getUser();
		if(user == null) {
			te.use(player);
			return true;
		} else if(user == player) {
			te.userQuit();
			return true;
		}
		return false;
    }

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileDeveloper();
	}
	
	private TileDeveloper safecast(TileEntity te) {
		return te == null ? null : (te instanceof TileDeveloper ? (TileDeveloper) te : null);
	}

	@Override
	public void initSubBlock() {
		addSubBlock(1, 0, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double[] getRotCenter() {
		return new double[] { 1, 0, 0.5 };
	}

}
