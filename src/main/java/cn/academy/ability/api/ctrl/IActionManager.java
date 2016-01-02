package cn.academy.ability.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;

/**
 * @author EAirPeter
 */
interface IActionManager {
    void startAction(SyncAction action);
    void endAction(SyncAction action);
    void abortAction(SyncAction action);
    SyncAction findAction(EntityPlayer player, Class clazz);
}
