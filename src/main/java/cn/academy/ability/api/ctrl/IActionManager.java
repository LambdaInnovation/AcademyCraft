/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
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
