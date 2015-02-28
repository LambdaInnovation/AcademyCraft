/**
 * 
 */
package cn.academy.ability.teleport.data;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import cn.academy.api.event.AbilityEvent;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Player location-teleport coordinate data. 
 * @author WeathFolD
 */
@RegistrationClass
@RegEventHandler
public class LocationData implements IExtendedEntityProperties {
	
	static final String IDENTIFIER = "tp_location";
	final EntityPlayer player;
	
	@SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
		if (event.entity instanceof EntityPlayer) {
            if (LocationData.get((EntityPlayer) event.entity) == null)
            	LocationData.register((EntityPlayer) event.entity);
        }
	}
	
	@SubscribeEvent
	public void onCategoryChanged(AbilityEvent.ChangeCategory event) {
		get(event.entityPlayer).locationList.clear();
		sync();
	}
	
	/**
	 * Saving struct.
	 */
	public static class Location {
		public final String name;
		public final int dimension;
		public final float x, y, z;
		
		public Location(NBTTagCompound tag, int i) {
			name = tag.getString("id_" + i);
			dimension = tag.getByte("dim_" + i);
			x = tag.getFloat("x_" + i);
			y = tag.getFloat("y_" + i);
			z = tag.getFloat("z_" + i);
		}
		
		public Location(String _name, int dim, float _x, float _y, float _z) {
			GenericUtils.assertObj(_name); //check if valid
			name = _name;
			dimension = dim;
			x = _x;
			y = _y;
			z = _z;
		}
		
		public Location(ByteBuf buf) {
			name = ByteBufUtils.readUTF8String(buf);
			dimension = buf.readByte();
			x = buf.readFloat();
			y = buf.readFloat();
			z = buf.readFloat();
		}
		
		void toNBT(NBTTagCompound tag, int i) {
			tag.setString("id_" + i, name);
			tag.setByte("dim_" + i, (byte) dimension);
			tag.setFloat("x_" + i, x);
			tag.setFloat("y_" + i, x);
			tag.setFloat("z_" + i, x);
		}
		
		void toBuf(ByteBuf buf) {
			ByteBufUtils.writeUTF8String(buf, name);
			buf.writeByte(dimension);
			buf.writeFloat(x).writeFloat(y).writeFloat(z);
		}
	}
	
	//May be modified by SyncMsg.
	List<Location> locationList = new ArrayList();

	public LocationData(EntityPlayer _player) {
		player = _player;
	}
	
	public static LocationData get(EntityPlayer player) {
		return (LocationData) player.getExtendedProperties(IDENTIFIER);
	}
	
	@SideOnly(Side.CLIENT)
	public void clientAdd(Location loc) {
		locationList.add(loc);
	}
	
	public static void register(EntityPlayer player) {
		player.registerExtendedProperties(IDENTIFIER, new LocationData(player));
	}
	
	public Location getLocation(int i) {
		return locationList.get(i);
	}
	
	public int getLocCount() {
		return locationList.size();
	}

	@Override
	public void saveNBTData(NBTTagCompound tag) {
		tag.setInteger("count", locationList.size());
	}

	@Override
	public void loadNBTData(NBTTagCompound tag) {
		int n = tag.getInteger("count");
		for(int i = 0; i < n; ++i) {
			locationList.add(new Location(tag, i));
		}
		//Sync to client right away
		sync();
	}

	@Override
	public void init(Entity entity, World world) {
		//NOPE
	}
	
	void sync() {
		AcademyCraft.netHandler.sendTo(new SyncMsg(this), (EntityPlayerMP) this.player);
	}

}
