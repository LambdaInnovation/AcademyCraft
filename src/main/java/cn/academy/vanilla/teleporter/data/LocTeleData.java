/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.data;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.SerializeType;
import cn.lambdalib.s11n.nbt.NBTS11n;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * The data of player's location teleport skill. Note that the data editing to
 * this class have effect in both sides. That is, the edit in client side will
 * be sync to server side. For that reason, you should be very careful so that
 * when editing client side data is always IN SYNC.
 * 
 * @author WeAthFolD
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class LocTeleData extends DataPart<EntityPlayer> {

    @SerializeIncluded
    private List<Location> locationList = new ArrayList<>();

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
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

    @Registrant
    @SerializeType
    @NetworkS11nType
    public static class Location {
        public String name;
        public int dimension;
        public float x, y, z;

        public Location(String _name, int dim, float _x, float _y, float _z) {
            name = _name;
            dimension = dim;
            x = _x;
            y = _y;
            z = _z;
        }

        public Location() {}

        public Location(NBTTagCompound tag) {
            this(tag.getString("n"), tag.getByte("d"), tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"));
        }

        public String formatCoords() {
            return String.format("(%.1f, %.1f, %.1f)", x, y, z);
        }
    }

}
