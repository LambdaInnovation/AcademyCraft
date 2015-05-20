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
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.SerializationManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
 * However no dynamic sync helper is provided. It's more convenient and flexible
 *  if you take advantage of AR's NetworkCall to sync stuffs (Either lazily or dynamically). 
 *  For that purpose, we have already provided the instance serializer for you.<br/>
 *  
 * When player is available the {@link DataPart#tick()} will get called every tick. You can 
 * process update stuffs within that method. <br/>
 * 
 * TODO If tick is too slow make selective ticking optimization <br/>
 * TODO Generalize this pattern if necessary <br/>
 * TODO Allow dynamic dispatch if necessary <br/>
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = DataPart.Serializer.class)
public abstract class DataPart {

	/**
	 * Internal sync flag, used to determine whether this part is init in client.
	 */
	boolean dirty = true;
	
	public PlayerData data;
	
	public DataPart() {}
	
	public abstract void tick();
	
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
	 * Client-only. Return true if this data has received the initial sync.
	 */
	@SideOnly(Side.CLIENT)
	protected boolean isSynced() {
		return !dirty;
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
