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

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.event.node.LinkUserEvent;
import cn.academy.energy.api.event.node.UnlinkUserEvent;
import cn.liutils.util.generic.RandUtils;

/**
 * Infinite Generator for debug
 * @author WeAthFolD
 */
public class BlockInfiniteGen extends ACBlockContainer {

	public BlockInfiniteGen() {
		super("infinite_generator", Material.rock, null);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileInfiniteGen();
	}
	
	// WILL GET REMOVED WHEN RELEASE
	@Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
		if(world.isRemote)
			return false;
		
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileInfiniteGen) {
			TileInfiniteGen gen = (TileInfiniteGen) te;
			if(WirelessHelper.isGeneratorLinked(gen)) {
				MinecraftForge.EVENT_BUS.post(new UnlinkUserEvent(gen));
				player.addChatMessage(new ChatComponentTranslation("Already linked, unlinking the generator."));
			} else {
				List<IWirelessNode> nodes = WirelessHelper.getNodesInRange(world, x, y, z);
				if(nodes.isEmpty()) {
					player.addChatMessage(new ChatComponentTranslation("Didn't find any node nearby, can't link."));
				} else {
					IWirelessNode node = nodes.get(RandUtils.rangei(0, nodes.size()));
					player.addChatMessage(new ChatComponentTranslation("Linking to a random node named " + node.getNodeName()));
					MinecraftForge.EVENT_BUS.post(new LinkUserEvent(gen, node));
				}
			}
			
			return true;
		}
		
		return false;
	}

}