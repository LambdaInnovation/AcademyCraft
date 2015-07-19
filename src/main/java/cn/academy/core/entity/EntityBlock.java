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
package cn.academy.core.entity;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderEntityBlock;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.entityx.event.CollideEvent;
import cn.liutils.entityx.event.CollideEvent.CollideHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * An entity that renders block.
 * @author WeAthFolD
 */
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityBlock extends EntityAdvanced {
	
	// For debug
	public int c;
	
	@RegEntity.Render
	public static RenderEntityBlock renderer;
	
	/**
	 * Create an EntityBlock from the block given in the coordinate.
	 * DOESN't set the coordinate, do it yourself!
	 */
	public static EntityBlock convert(World world, int x, int y, int z) {
		EntityBlock ret = new EntityBlock(world);
		ret.setBlock(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
		ret.setTileEntity(world.getTileEntity(x, y, z));
		return ret;
	}
	
	static final int BLOCKID = 4, META = 5;
	
	public Block block;
	public int metadata = 0;
	public TileEntity tileEntity;
	
	// other
	public boolean placeWhenCollide = true;

	public EntityBlock(World world) {
		super(world);
		ignoreFrustumCheck = true;
	}
	
	@Override
	public void entityInit() {
		dataWatcher.addObject(BLOCKID, Short.valueOf((short) 0));
		dataWatcher.addObject(META, Byte.valueOf((byte) 0));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(worldObj.isRemote) {
			block = Block.getBlockById(dataWatcher.getWatchableObjectShort(BLOCKID));
			metadata = dataWatcher.getWatchableObjectByte(META);
		} else {
			if(block != null) {
				dataWatcher.updateObject(BLOCKID, Short.valueOf((short) Block.getIdFromBlock(block)));
				dataWatcher.updateObject(META, Byte.valueOf((byte) metadata));
				
				if(tileEntity != null)
					tileEntity.blockMetadata = metadata;
			}
		}
	}
	
	@Override
	public void onFirstUpdate() {
		if(placeWhenCollide) {
			this.regEventHandler(new CollideHandler() {

				@Override
				public void onEvent(CollideEvent event) {
					// TODO
				}
				
			});
		}
		
		if(!worldObj.isRemote && tileEntity != null) {
			System.out.println("Sending te sync");
			try {
				NBTTagCompound tag = new NBTTagCompound();
				tileEntity.writeToNBT(tag);
				
				syncTileEntity(tileEntity.getClass().getName(), tag);
			} catch(Exception e) {
				AcademyCraft.log.error("Error syncing te", e);
			}
		}
	}

	public void setBlock(Block _block) {
		block = _block;
	}
	
	public void setBlock(Block _block, int _metadata) {
		block = _block;
		metadata = _metadata;
	}
	
	public void setTileEntity(TileEntity _te) {
		tileEntity = _te;
	}
	
	public boolean shouldRender() {
		return block != null;
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return (block != null && block.getRenderBlockPass() == pass) || 
				(tileEntity != null && tileEntity.shouldRenderInPass(pass));
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	private void syncTileEntity(@Data String klassName, @Data NBTTagCompound initTag) {
		System.out.println("SyncTileEntity");
		try {
			TileEntity te = (TileEntity) Class.forName(klassName).newInstance();
			te.readFromNBT(initTag);
			te.setWorldObj(worldObj);
			tileEntity = te;
		} catch(Exception e) {
			AcademyCraft.log.error("Unable to sync tileEntity " + klassName, e);
		}
	}

}
