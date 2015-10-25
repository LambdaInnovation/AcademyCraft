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
package cn.academy.support;

import java.util.List;

import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.client.gui.GuiLinkToNode;
import cn.academy.support.ic2.TileEUOutput;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.liutils.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public abstract class BlockConverterBase extends ACBlockContainer {
	
	public static class Item extends ItemBlock {
		
		BlockConverterBase converter;

		public Item(Block block) {
			super(block);
			converter = (BlockConverterBase) block;
		}
		
		@SideOnly(Side.CLIENT)
	    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean idk) {
			list.add(StatCollector.translateToLocalFormatted("ac.converter.desc_template", converter.from, converter.to));
		}
		
	}

	public final Class<? extends TileEntity> tileType;
	public final String from, to;
	
	public BlockConverterBase(String name, String _from, String _to, Class<? extends TileEntity> _tileType) {
		super(name, Material.rock);
		from = _from;
		to = _to;
		tileType = _tileType;
		setHarvestLevel("pickaxe", 0);
		setHardness(2.5f);
	}

	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
		TileEntity te = WorldUtils.getTileEntity(world, x, y, z, tileType);
		if(te != null && !player.isSneaking()) {
			if(!world.isRemote) {
				displayGui(te);
			}
			return true;
		}
		return false;
	}
	
	@SideOnly(Side.CLIENT)
	private void displayGui(TileEntity te) {
		Minecraft.getMinecraft().displayGuiScreen(new GuiLinkToNode((IWirelessUser) te));
	}

}
