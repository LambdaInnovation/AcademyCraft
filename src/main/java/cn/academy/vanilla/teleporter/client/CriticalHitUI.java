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
package cn.academy.vanilla.teleporter.client;

import net.minecraft.client.gui.ScaledResolution;
import cn.academy.vanilla.teleporter.util.TPAttackHelper.TPCritHitEvent;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.api.gui.AuxGui;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@Registrant
public class CriticalHitUI extends AuxGui {

	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
	}
	
	@RegEventHandler(Bus.Forge)
	public static class Trigger {
		@SubscribeEvent
		public void onCritHit(TPCritHitEvent event) {
			if(event.player.worldObj.isRemote) {
				System.out.printf("A level %d critical hit has happened!\n", event.level);
			}
		}
	}

}
