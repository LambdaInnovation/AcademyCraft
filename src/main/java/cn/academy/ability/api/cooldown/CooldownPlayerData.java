package cn.academy.ability.api.cooldown;

import cn.lambdalib.util.mc.PlayerUtils;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Manages cooldown data for one player on server side.
 * @author EAirPeter
 */
public class CooldownPlayerData {

    private final String name;

    private final Map<Integer, Cd> map = new HashMap<>();

    private EntityPlayerMP player = null;

    CooldownPlayerData(String pName) {
        name = pName;
    }


    void setCd(int id, int cd) {
        if (cd == 0)
            map.remove(id);
        else
            map.put(id, new Cd(cd));
        if (player != null)
            CooldownManager.cNetSetCd(player, id, cd);
    }

    boolean isInCd(int id) {
        return map.containsKey(id);
    }

    public boolean tick() {
        boolean sendAll = false;
        if (player == null || player.isDead) {
            player = FMLCommonHandler.instance().getMinecraftServerInstance().
                getConfigurationManager().func_152612_a(name);
            sendAll = player != null;
        }
        for (Iterator<Entry<Integer, Cd>> i = map.entrySet().iterator();
            i.hasNext(); )
        {
            Entry<Integer, Cd> e = i.next();
            if (--e.getValue().cd <= 0) {
                i.remove();
                if (player != null)
                    CooldownManager.cNetSetCd(player, e.getKey(), 0);
            }
            else if (sendAll)
                CooldownManager.cNetSetCd(player, e.getKey(), e.getValue().cd);
        }
        return map.isEmpty();
    }

    public void readFromNBT(NBTTagList tag) {
        Preconditions.checkState(map.isEmpty());
        for (int i = 0; i < tag.tagCount(); ++i) {
            NBTTagCompound sub = tag.getCompoundTagAt(i);
            map.put(sub.getInteger("id"), new Cd(sub.getInteger("cd")));
        }
    }

    public void writeToNBT(NBTTagList tag) {
        Preconditions.checkState(!map.isEmpty());
        for (Entry<Integer, Cd> e : map.entrySet()) {
            NBTTagCompound sub = new NBTTagCompound();
            sub.setInteger("id", e.getKey());
            sub.setInteger("cd", e.getValue().cd);
            tag.appendTag(sub);
        }
    }

    private static class Cd {
        public int cd;

        public Cd(int pCd) {
            cd = pCd;
        }
    }

}
