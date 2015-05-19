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

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.RegSerializable;
import cn.annoreg.mc.s11n.SerializationManager;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.ReflectUtils;

/**
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = DataPart.Serializer.class)
public abstract class DataPart {

	public PlayerData data;
	
	boolean dirty;
	
	public DataPart() {
		dirty = GenericUtils.getSide() == Side.SERVER;
	}
	
	/**
	 * If this flag is false, client sync requests will ALWAYS get discarded.
	 * This is used to prevent stupid mistakes. If your DataPart need to sync from
	 * client using the 'dirty' mechanism, set this to true.
	 */
	protected boolean allowClientSync = false;
	
	/**
	 * If this is enabled, will load the public field(If possible via DataSerializer) automatically.
	 */
	protected boolean autoSync = false;
	
	/**
	 * Mark this data as dirty for next tick's network sync.
	 */
	public void markDirty() {
		dirty = true;
	}
	
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
	
	public void fromNBT(NBTTagCompound tag) {
		if(autoSync) {
			try {
				ReflectUtils.fromNBT(this, tag);
			} catch (Exception e) {
				AcademyCraft.log.error("Exception occured handling nbt loading of " + this.getClass());
				e.printStackTrace();
			}
		}
	}
	
	public NBTTagCompound toNBT() {
		if(autoSync) {
			try {
				return ReflectUtils.toNBT(this);
			} catch(Exception e) {
				AcademyCraft.log.error("Exception converting nbt of " + this.getClass());
				e.printStackTrace();
			}
		}
		return null;
	}
	
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
