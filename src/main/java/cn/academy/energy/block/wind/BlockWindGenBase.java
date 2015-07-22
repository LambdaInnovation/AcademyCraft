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
package cn.academy.energy.block.wind;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.block.ACBlockMulti;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.client.gui.wind.GuiWindGenBase;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class BlockWindGenBase extends ACBlockMulti {
	
	@RegGuiHandler
	public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		@Override
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			ContainerWindGenBase container = (ContainerWindGenBase) getServerContainer(player, world, x, y, z);
			return container == null ? null : new GuiWindGenBase(container);
		}
		
		@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileWindGenBase tile = locate(world, x, y, z);
			return tile == null ? null : new ContainerWindGenBase(player, tile);
		}
		
		private TileWindGenBase locate(World world, int x, int y, int z) {
			Block b = world.getBlock(x, y, z);
			if(!(b == ModuleEnergy.windgenBase))
				return null;
			
			TileEntity te = ModuleEnergy.windgenBase.getOriginTile(world, x, y, z);
			return te instanceof TileWindGenBase ? (TileWindGenBase) te : null;
		}
	};

	public BlockWindGenBase() {
		super("windgen_base", Material.rock);
		setHardness(4.0f);
		setHarvestLevel("pickaxe", 2);
		addSubBlock(new int[][] {
			{ 0, 1, 0 }
		});
		finishInit();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileWindGenBase();
	}

	@Override
	public double[] getRotCenter() {
		return new double[] { 0.5, 0, 0.5 };
	}
	
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(!player.isSneaking()) {
        	guiHandler.openGuiContainer(player, world, x, y, z);
            return true;
        }
        return false;
    }

}
