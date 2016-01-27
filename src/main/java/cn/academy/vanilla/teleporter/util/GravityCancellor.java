/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.util;

import cn.lambdalib.util.deprecated.LIHandler;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public class GravityCancellor extends LIHandler<ClientTickEvent> {

    final EntityPlayer p;
    final int ticks;
    int ticker = 0;

    public GravityCancellor(EntityPlayer _p, int _ticks) {
        p = _p;
        ticks = _ticks;
    }

    @Override
    protected boolean onEvent(ClientTickEvent event) {
        if (p.isDead || (++ticker == ticks)) {
            this.setDead();
        } else {
            if (!p.capabilities.isFlying) {
                if (!p.onGround) {
                    p.motionY += 0.036;
                }
            }
        }
        return true;
    }

}
