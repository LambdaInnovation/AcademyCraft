package cn.academy.ability.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import cn.academy.core.proxy.ProxyHelper;
import cn.annoreg.mc.s11n.DataSerializer;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.SerializationManager;
import cn.annoreg.mc.s11n.StorageOption.Option;

/**
 * For serialization of any SyncAction.
 * Even though this is just an instance serializer, it serializes the content of an action,
 * in case that the receiver does not has this instance yet.
 * TODO might be able to avoid this?
 * Note that this class uses DataSerializer for the exact class of an action to 
 * synchronize user data, if a DataSerializer is available. But no more data synchronization is
 * done automatically after that, while you can easily do it yourself.
 * @author acaly
 *
 */
public class ActionSerializer implements InstanceSerializer<SyncAction> {

	@Override
	public SyncAction readInstance(NBTBase nbt) throws Exception {
        NBTTagCompound c = (NBTTagCompound) nbt;
	    SyncAction ret = ProxyHelper.get().getActionFromId(c.getString("id"));
	    if (ret != null) {
	        return ret;
	    } else {
    	    ret = readInstanceInit(nbt);
    	    return ret;
	    }
	}

	@Override
	public NBTBase writeInstance(SyncAction obj) throws Exception {
		return writeInstanceInit(obj); //always write init data
	}

    public SyncAction readInstanceInit(NBTBase nbt) throws Exception {
        NBTTagCompound c = (NBTTagCompound) nbt;
        
        //Get fields in compound
        String id = c.getString("id");
        String className = c.getString("class");
        EntityPlayer player = (EntityPlayer) SerializationManager.INSTANCE.deserialize(null, 
                c.getTag("player"), Option.INSTANCE);
        NBTBase ud = null;
        if (c.hasKey("data")) {
            ud = c.getTag("data");
        }
        
        //Create instance
        Class clazz = Class.forName(className);
        SyncAction action = (SyncAction) clazz.getConstructor(EntityPlayer.class).newInstance(player);

        //Register it
        ProxyHelper.get().registerAction(id, action);
        
        //Deserialize user data
        if (ud != null) {
            SerializationManager.INSTANCE.deserialize(action, ud, Option.DATA);
        }
        
        return action;
    }

    public NBTBase writeInstanceInit(SyncAction obj) throws Exception {
        //Set fields
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", obj.id);
        nbt.setString("class", obj.getClass().getName());
        nbt.setTag("player", SerializationManager.INSTANCE.serialize(obj.player, Option.INSTANCE));
        
        //User data
        DataSerializer uds = SerializationManager.INSTANCE.getDataSerializer(obj.getClass());
        if (uds != null) {
            nbt.setTag("data", uds.writeData(obj));
        }
        
        return nbt;
    }

}
