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
package cn.academy.core.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.SerializationManager;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cpw.mods.fml.relauncher.Side;

/**
 * DataPart represents a single tickable instance attached on an EntityPlayer.
 *  It is driven by the PlayerData of this player. <br/>
 *  
 * The DataPart is attached statically via {@link PlayerData#register(String, Class)}
 *  method or @RegDataPart (This should be done at init stages), and is automatically constructed when an
 *  EntityPlayer enters the world. At server, the {@link DataPart#fromNBT(NBTTagCompound)}
 *  method will be called right away, and the NBT will also be sync to client ASAP. 
 *  Also when the world is being saved, the {@link DataPart#toNBT()} will be called to save stuffs
 *   in server.
 *  <br/>
 * 
 * A simple sync helper is provided. You can use sync() in both CLIENT and SERVER side to make a new NBT
 *  synchronization. However, for complex syncs you might want to consider using NetworkCall.
 *  
 * When player is available the {@link DataPart#tick()} will get called every tick. You can 
 * process update stuffs within that method. <br/>
 * 
 * TODO If tick is too slow make selective ticking optimization <br/>
 * TODO Generalize this pattern if necessary <br/>
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = DataPart.Serializer.class)
public abstract class DataPart {

	/**
	 * Internal sync flag, used to determine whether this part is init in client.
	 */
	boolean dirty = true;
	
	int tickUntilQuery = 0;
	
	/**
	 * The player instance when this data is available. Do NOT modify this field!
	 */
	PlayerData data;
	
	public DataPart() {}
	
	/**
	 * Invoked every tick
	 */
	public void tick() {}
	
	/**
	 * Invoked before the data is saved. Can do validation and cleanup.
	 */
	public void preSaving() {}
	
	public EntityPlayer getPlayer() {
		return data.player;
	}
	
	public boolean isRemote() {
		return getPlayer().worldObj.isRemote;
	}
	
	public <T extends DataPart> T getPart(String name) {
		return data.getPart(name);
	}
	
	public String getName() {
		return data.getName(this);
	}
	
	/**
	 * Return true if this data has received the initial sync.
	 * ALWAYS true in server.
	 */
	protected boolean isSynced() {
		return !dirty;
	}
	
	protected void sync() {
		if(isRemote()) {
			syncFromClient(toNBT());
		} else {
			syncFromServer(getPlayer(), toNBT());
		}
	}
	
	@RegNetworkCall(side = Side.SERVER)
	private void syncFromClient(@Data NBTTagCompound tag) {
		fromNBT(tag);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	private void syncFromServer(@Target EntityPlayer player, @Data NBTTagCompound tag) {
		fromNBT(tag);
	}
	
	public abstract void fromNBT(NBTTagCompound tag);
	
	public abstract NBTTagCompound toNBT();
	
	public static class Serializer implements InstanceSerializer<DataPart> {
		
		InstanceSerializer entitySer = SerializationManager.INSTANCE.getInstanceSerializer(Entity.class);

		@Override
		public DataPart readInstance(NBTBase nbt) throws Exception {
			NBTTagCompound tag = (NBTTagCompound) nbt;
			NBTBase entityTag = tag.getTag("e");
			if(entityTag != null) {
				Entity e = (Entity) entitySer.readInstance(entityTag);
				if(e instanceof EntityPlayer) {
					return PlayerData.get((EntityPlayer) e).getPart(tag.getString("n"));
				}
			}
			
			return null;
		}

		@Override
		public NBTBase writeInstance(DataPart obj) throws Exception {
			NBTTagCompound ret = new NBTTagCompound();
			
			NBTBase entityTag = entitySer.writeInstance(obj.getPlayer());
			
			ret.setTag("e", entityTag);
			ret.setString("n", obj.getName());
			
			return ret;
		}
		
	}
	
}
