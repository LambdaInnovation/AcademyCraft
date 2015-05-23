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
package cn.academy.energy.internal;

import cn.academy.energy.api.event.CreateNetworkEvent;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegEventHandler
public class WirelessSystem {
	
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event) {
		if(!event.world.isRemote && event.phase == Phase.END) {
			WiWorldData.get(event.world).tick();
		}
	}
	
	@SubscribeEvent
	public void onCreateNet(CreateNetworkEvent event) {
		
	}
	
}
