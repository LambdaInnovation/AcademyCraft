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
package cn.academy.energy.internal;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;

/**
 * @author WeAthFolD
 *
 */
public class VBlocks {
	
	static abstract class VBlock<T extends IWirelessTile> {
		
		protected final World world;
		protected final int x, y, z;
		protected final boolean ignoreChunk;
		
		public VBlock(TileEntity te, boolean _ignoreChunk) {
			x = te.xCoord;
			y = te.yCoord;
			z = te.zCoord;
			world = te.getWorldObj();
			ignoreChunk = _ignoreChunk;
		}
		
		public VBlock(World _world, NBTTagCompound tag, boolean _ignoreChunk) {
			world = _world;
			x = tag.getInteger("x");
			y = tag.getInteger("y");
			z = tag.getInteger("z");
			ignoreChunk = _ignoreChunk;
		}
		
		public boolean isLoaded() {
			return world.getChunkProvider().chunkExists(x >> 4, z >> 4);
		}
		
		public T get() {
			if(!ignoreChunk && !isLoaded())
				return null;
			
			TileEntity te = world.getTileEntity(x, y, z);
			if(te == null || !isAcceptable(te)) {
				return null;
			}
			return (T) te;
		}
		
		protected abstract boolean isAcceptable(TileEntity tile);
		
	}
	
	static class VWMatrix extends VBlock<IWirelessMatrix> {

		public VWMatrix(IWirelessMatrix te) {
			super((TileEntity) te, true);
		}
		
		public VWMatrix(World world, NBTTagCompound tag) {
			super(world, tag, true);
		}

		@Override
		protected boolean isAcceptable(TileEntity tile) {
			return tile instanceof IWirelessMatrix;
		}
		
	}
	
	static class VWNode extends VBlock<IWirelessNode> {
		
		public VWNode(IWirelessNode te) {
			super((TileEntity) te, false);
		}
		
		public VWNode(World world, NBTTagCompound tag) {
			super(world, tag, false);
		}

		@Override
		protected boolean isAcceptable(TileEntity tile) {
			return tile instanceof IWirelessNode;
		}
		
	}
	
	
	
	
}
