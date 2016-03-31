package cn.academy.ability.api.cooldown;

import com.google.common.base.Preconditions;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author EAirPeter
 */
public class CooldownWorldData extends WorldSavedData {

    public static final String ID = "AC_COOLDOWNDATA";

    public static final byte ID_COMPOUND = new NBTTagCompound().getId();

    private Map<String, CooldownPlayerData> map = new HashMap<>();

    public CooldownWorldData(String id) {
        super(id);
    }

    public void setCd(String name, int id, int cd) {
        System.out.println("CM::SETCD " + name + " " + id + " " + cd);
        CooldownPlayerData cpd = map.get(name);
        if (cpd == null) {
            cpd = new CooldownPlayerData(name);
            cpd.setCd(id, cd);
            map.put(name, cpd);
        }
        else
            cpd.setCd(id, cd);
    }

    public boolean isInCd(String name, int id) {
        System.out.println("CM::ISINCD " + name + " " + id);
        CooldownPlayerData cpd = map.get(name);
        return cpd != null && cpd.isInCd(id);
    }

    public void clearCd(String name) {
        System.out.println("CM::CLEARCD " + name);
        map.remove(name);
    }

    public void tick() {
        for (Iterator<CooldownPlayerData> i = map.values().iterator(); i.hasNext(); )
            if (i.next().tick())
                i.remove();
    }

    public static CooldownWorldData load(World world) {
        CooldownWorldData ret =
            (CooldownWorldData) world.loadItemData(CooldownWorldData.class, ID);
        if (ret == null)
            world.setItemData(ID, ret = new CooldownWorldData(ID));
        return ret;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        Preconditions.checkState(map.isEmpty());
        NBTTagList list = tag.getTagList("data", ID_COMPOUND);
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound sub = list.getCompoundTagAt(i);
            String name = sub.getString("name");
            CooldownPlayerData cpd = new CooldownPlayerData(name);
            cpd.readFromNBT(sub.getTagList("data", ID_COMPOUND));
            map.put(name, cpd);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Entry<String, CooldownPlayerData> e : map.entrySet()) {
            NBTTagCompound sub = new NBTTagCompound();
            sub.setString("name", e.getKey());
            NBTTagList sublist = new NBTTagList();
            e.getValue().writeToNBT(sublist);
            sub.setTag("data", sublist);
            list.appendTag(sub);
        }
        tag.setTag("data", list);
    }
}
