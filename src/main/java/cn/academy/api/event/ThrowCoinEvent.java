/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.event;

import cn.academy.misc.entity.EntityThrowingCoin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Fired when a player starts throwing a coin.
 * @author WeathFolD
 */
public class ThrowCoinEvent extends PlayerEvent {

	public final EntityThrowingCoin coin;
	
	public ThrowCoinEvent(EntityPlayer player, EntityThrowingCoin _coin) {
		super(player);
		coin = _coin;
	}

}
