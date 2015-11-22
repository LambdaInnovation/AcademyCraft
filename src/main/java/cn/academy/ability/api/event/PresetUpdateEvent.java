/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.api.event;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired BOTH CLIENT AND SERVER. After player has updated his preset.
 * This event is used to indicate that a preset has been UPDATED. It is not necessarily
 * fired only when player edits his preset using UI, but also when data are syncedSingle from
 * server/client.
 * @author WeAthFolD
 */
public class PresetUpdateEvent extends AbilityEvent {

	public PresetUpdateEvent(EntityPlayer _player) {
		super(_player);
	}

}
