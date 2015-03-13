/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.liutils.core.event.eventhandler.LIFMLGameEventDispatcher;
import cn.liutils.core.event.eventhandler.LIHandler;
import cn.liutils.util.ClientUtils;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * @author WeathFolD
 *
 */
public class LoopSoundPlayer extends LIHandler<TickEvent> {
	
	final EntityPlayer player;
	
	int ticker;
	final int life, rate;
	final String snd;	
	ResourceLocation res;
	
	public LoopSoundPlayer(EntityPlayer _player, String sound, int _life, int _loopRate) {
		player = _player;
		life = _life;
		rate = _loopRate;
		snd = sound;
		res = new ResourceLocation(sound);
	}

	@Override
	protected boolean onEvent(TickEvent event) {
		if(event.phase == Phase.START) return true;
		//System.out.println("Tick " + (event instanceof ServerTickEvent));
		if(ticker++ % rate == 0) {
			
			if(event instanceof ServerTickEvent) {
				player.playSound(snd, 0.5f, 1.0f);
			} else {
				ClientUtils.playSound(res, 1.0f);
			}
		}
		if(ticker >= life) this.setDead();
		return true;
	}
	
	public static void dispatch(LoopSoundPlayer lsp) {
		if(lsp.player.worldObj.isRemote) {
			LIFMLGameEventDispatcher.INSTANCE.registerClientTick(lsp);
		} else {
			LIFMLGameEventDispatcher.INSTANCE.registerServerTick(lsp);
		}
	}

}