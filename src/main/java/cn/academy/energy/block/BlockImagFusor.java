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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.block.ACBlockContainer;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class BlockImagFusor extends ACBlockContainer {

	public BlockImagFusor() {
		super("imag_fusor", Material.rock, guiHandler);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileImagFusor();
	}
	
	@RegGuiHandler
	public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		@Override
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileImagFusor te = check(world, x, y, z);
			System.out.println("Open client " + te);
			return te == null ? null : new GuiImagFusor(new ContainerImagFusor(te, player));
		}
		
		@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileImagFusor te = check(world, x, y, z);
			System.out.println("Open server " + te);
			return te == null ? null : new ContainerImagFusor(te, player);
		}
		
		private TileImagFusor check(World world, int x, int y, int z) {
			TileEntity te = world.getTileEntity(x, y, z);
			return (TileImagFusor) (te instanceof TileImagFusor ? te : null);
		}
	};

}
