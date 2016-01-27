/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.event;

import cn.academy.vanilla.electromaster.entity.EntityCoinThrowing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * @author WeAthFolD
 */
public class CoinThrowEvent extends PlayerEvent {
    
    public final EntityCoinThrowing coin;

    public CoinThrowEvent(EntityPlayer _player, EntityCoinThrowing _coin) {
        super(_player);
        coin  = _coin;
    }

}
