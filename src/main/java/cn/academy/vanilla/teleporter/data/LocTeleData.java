/**
 * 
 */
package cn.academy.vanilla.teleporter.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.DataSerializer;
import cn.lambdalib.networkcall.s11n.RegSerializable;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;

/**
 * The data of player's location teleport skill. Note that the data editing to
 * this class have effect in both sides. That is, the edit in client side will
 * be sync to server side. For that reason, you should be very careful so that
 * when editing client side data is always IN SYNC.
 * 
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("loctele")
public class LocTeleData extends DataPart<EntityPlayer> {

	private List<Location> locationList = new ArrayList();

	public static LocTeleData get(EntityPlayer player) {
		return EntityData.get(player).getPart(LocTeleData.class);
	}

	public int getLocCount() {
		return locationList.size();
	}

	public void removeAt(int id) {
		locationList.remove(id);
		sync();
	}

	public void rename(int id, String newName) {
		Location original = locationList.get(id);
		locationList.set(id, new Location(newName, original.dimension, original.x, original.y, original.z));
		sync();
	}

	public void add(Location l) {
		locationList.add(l);
		sync();
	}

	public Location get(int id) {
		return locationList.get(id);
	}

	@Override
	public void fromNBT(NBTTagCompound tag) {
		locationList.clear();
		NBTTagList list = (NBTTagList) tag.getTag("l");

		for (int i = 0; i < list.tagCount(); ++i) {
			locationList.add(new Location(list.getCompoundTagAt(i)));
		}
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();

		NBTTagList nList = new NBTTagList();
		for (int i = 0; i < locationList.size(); ++i) {
			nList.appendTag(locationList.get(i).toNBT());
		}
		tag.setTag("l", nList);
		return tag;
	}

	@RegSerializable(data = LocationSerializer.class)
	public static class Location {
		public final String name;
		public final int dimension;
		public final float x, y, z;

		public Location(String _name, int dim, float _x, float _y, float _z) {
			name = _name;
			dimension = dim;
			x = _x;
			y = _y;
			z = _z;
		}

		public Location(NBTTagCompound tag) {
			this(tag.getString("n"), tag.getByte("d"), tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"));
		}

		public String formatCoords() {
			return String.format("(%.1f, %.1f, %.1f)", x, y, z);
		}

		NBTTagCompound toNBT() {
			NBTTagCompound ret = new NBTTagCompound();

			ret.setString("n", name);
			ret.setByte("d", (byte) dimension);
			ret.setFloat("x", x);
			ret.setFloat("y", y);
			ret.setFloat("z", z);

			return ret;
		}
	}

	public static class LocationSerializer implements DataSerializer<Location> {

		@Override
		public Location readData(NBTBase nbt, Location obj) throws Exception {
			return new Location((NBTTagCompound) nbt);
		}

		@Override
		public NBTBase writeData(Location obj) throws Exception {
			return obj.toNBT();
		}

	}

}
