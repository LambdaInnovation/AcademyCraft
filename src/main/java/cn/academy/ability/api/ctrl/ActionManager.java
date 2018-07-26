/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.ctrl;

import cn.lambdalib2.annoreg.core.Registrant;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n.NetworkS11nType;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/**
 * @author EAirPeter
 */
@Registrant
@NetworkS11nType
public class ActionManager {

    static final String
            M_START_SVR = "0",
            M_END_SVR = "1",
            M_ABORT_SVR = "2",
            M_ABORT_PLAYER_SVR = "3",
            M_START_CLIENT = "4",
            M_UPDATE_CLIENT = "5",
            M_END_CLIENT = "6",
            M_ABORT_CLIENT = "7";

    
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
    @Listener(channel=M_START_SVR, side=Side.SERVER)
    private static void startAtServer(EntityPlayer player, String className, NBTTagCompound tag) {
        AMS.startFromClient(player, className, tag);
    }

    @Listener(channel=M_END_SVR, side=Side.SERVER)
    private static void endAtServer(EntityPlayer player, String uuid) {
        AMS.endFromClient(player, UUID.fromString(uuid));
    }

    @Listener(channel=M_ABORT_SVR, side=Side.SERVER)
    private static void abortAtServer(EntityPlayer player, String uuid) {
        AMS.abortFromClient(player, UUID.fromString(uuid));
    }

    @Listener(channel=M_ABORT_PLAYER_SVR, side=Side.SERVER)
    private static void abortPlayerAtServer(EntityPlayer player) {
        AMS.abortPlayer(player);
    }

    @Listener(channel=M_START_CLIENT, side=Side.CLIENT)
    private static void startAtClient(EntityPlayer player, String className, NBTTagCompound tag) {
        AMC.startFromServer(player, className, tag);
    }

    @Listener(channel=M_UPDATE_CLIENT, side=Side.CLIENT)
    private static void updateAtClient(String uuid, NBTTagCompound tag) {
        AMC.updateFromServer(UUID.fromString(uuid), tag);
    }

    @Listener(channel=M_END_CLIENT, side=Side.CLIENT)
    private static void endAtClient(String uuid, NBTTagCompound tag) {
        AMC.endFromServer(UUID.fromString(uuid), tag);
    }

    @Listener(channel=M_ABORT_CLIENT, side=Side.CLIENT)
    private static void abortAtClient(String uuid, NBTTagCompound tag) {
        AMC.abortFromServer(UUID.fromString(uuid), tag);
    }
    
}
