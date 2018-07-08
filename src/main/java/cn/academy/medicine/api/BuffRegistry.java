package cn.academy.medicine.api;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegPostInitCallback;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Registrant
public class BuffRegistry {
    private static List<Class<? extends Buff>> buffTypes = new ArrayList<>();

    public void _register(Class<? extends Buff> klass){
        buffTypes.add(klass);
    }

    @RegPostInitCallback
    public static void _init(){
        buffTypes.sort(Comparator.comparing(Class::toString));
    }

    public static void writeBuff(Buff buff, NBTTagCompound tag){
        int id = buffTypes.indexOf(buff.getClass());
        tag.setInteger("id", id);
        buff.store(tag);
    }

    public static Buff readBuff(NBTTagCompound tag){
        int id = tag.getInteger("id");
        Buff buff = null;
        try {
            buff = buffTypes.get(id).newInstance();
            buff.load(tag);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return buff;
    }
}
