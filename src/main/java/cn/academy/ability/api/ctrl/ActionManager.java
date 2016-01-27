package cn.academy.ability.api.ctrl;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/**
 * @author EAirPeter
 */
@Registrant
public class ActionManager {
    
    private static final AMServer AMS = new AMServer();
    private static final AMClient AMC = new AMClient();
    
    public static void startAction(SyncAction action) {
        getActionManager().startAction(action);
    }
    
    public static void endAction(SyncAction action) {
        getActionManager().endAction(action);
    }
    
    public static void abortAction(SyncAction action) {
        getActionManager().abortAction(action);
    }
    
    public static <T extends SyncAction> T findAction(EntityPlayer player, Class<T> clazz) {
        return (T) getActionManager().findAction(player, clazz);
    }
    
    private static IActionManager getActionManager() {
        return FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER) ? AMS : AMC;
    }
    
    //NETWORK CALLS
    @RegNetworkCall(side = Side.SERVER)
    static void startAtServer(@Instance EntityPlayer player, @Data String className, @Data NBTTagCompound tag) {
        AMS.startFromClient(player, className, tag);
    }

    @RegNetworkCall(side = Side.SERVER)
    static void endAtServer(@Instance EntityPlayer player, @Data String uuid) {
        AMS.endFromClient(player, UUID.fromString(uuid));
    }
    
    @RegNetworkCall(side = Side.SERVER)
    static void abortAtServer(@Instance EntityPlayer player, @Data String uuid) {
        AMS.abortFromClient(player, UUID.fromString(uuid));
    }
    
    @RegNetworkCall(side = Side.SERVER)
    static void abortPlayerAtServer(@Instance EntityPlayer player) {
        AMS.abortPlayer(player);
    };
    
    @RegNetworkCall(side = Side.CLIENT)
    static void startAtClient(@Instance EntityPlayer player, @Data String className, @Data NBTTagCompound tag) {
        AMC.startFromServer(player, className, tag);
    }

    @RegNetworkCall(side = Side.CLIENT)
    static void updateAtClient(@Data String uuid, @Data NBTTagCompound tag) {
        AMC.updateFromServer(UUID.fromString(uuid), tag);
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    static void endAtClient(@Data String uuid, @Data NBTTagCompound tag) {
        AMC.endFromServer(UUID.fromString(uuid), tag);
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    static void abortAtClient(@Data String uuid, @Data NBTTagCompound tag) {
        AMC.abortFromServer(UUID.fromString(uuid), tag);
    }
    
}
