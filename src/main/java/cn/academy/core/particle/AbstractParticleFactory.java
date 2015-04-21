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
package cn.academy.core.particle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.world.World;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

/**
 * @author WeAthFolD
 */
@RegistrationClass
public abstract class AbstractParticleFactory {
	
	static final int MAX_POOL_SIZE = 1000;
	
	static List<Particle> alive = new ArrayList(), dead = new ArrayList();
	
	public abstract Particle next();
	
	protected final Particle queryParticle() {
		Particle ret;
		if(!dead.isEmpty()) {
			Iterator<Particle> iter = dead.iterator();
			ret = iter.next();
			iter.remove();
		} else {
			ret = new Particle();
		}
		
		if(alive.size() < MAX_POOL_SIZE) {
			alive.add(ret);
		}
		
		ret.isDead = false;
		ret.ticksExisted = 0;
		ret.resetEntityX();
		ret.reset();
		return ret;
	}
	
	@RegEventHandler(Bus.FML)
	public static class EventHandlers {
		static final int UPDATE_RATE = 40;
		int ticker;
		
		@SubscribeEvent
		public void onClientTick(ClientTickEvent event) {
			if(event.phase == Phase.END && ++ticker == UPDATE_RATE) {
				ticker = 0;
				
				Iterator<Particle> iter = alive.iterator();
				while(iter.hasNext()) {
					Particle p = iter.next();
					if(p.isDead) {
						iter.remove();
						if(dead.size() < MAX_POOL_SIZE) {
							dead.add(p);
						}
					}
				}
				//System.out.println("GC: " + alive.size() + " / " + dead.size());
			}
		}
	}
	
}
