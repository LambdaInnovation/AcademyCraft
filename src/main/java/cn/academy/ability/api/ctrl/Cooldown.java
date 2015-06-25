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
package cn.academy.ability.api.ctrl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import cn.academy.ability.api.Controllable;
import cn.liutils.util.client.ClientUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Class that handles cooldown. Currently, the cooldown in SERVER will be completely ignored, counting only
 * 	client cooldown on SkillInstance startup. But you can still visit the method to avoid side dependency.
 * @author WeAthFolD
 */
public class Cooldown {
	
	/**
	 * The current cooldown data map. Direct manipulation should be avoided, this
	 *  is opened just for visit of reading (like UI drawings)
	 */
	public static final Map<Controllable, Integer> cooldown = new HashMap();
	
    public static void setCooldown(Controllable c, int cd) {
    	cooldown.put(c, cd);
    }
    
    public static boolean isInCooldown(Controllable c) {
    	return cooldown.containsKey(c);
    }
    
    private static void updateCooldown() {
    	Iterator< Entry<Controllable, Integer> > iter = cooldown.entrySet().iterator();
    	
    	while(iter.hasNext()) {
    		Entry< Controllable, Integer > entry = iter.next();
    		if(entry.getValue() == 0) {
    			iter.remove();
    		} else {
    			entry.setValue(entry.getValue() - 1);
    		}
    	}
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientTick(ClientTickEvent event) {
    	if(event.phase == Phase.END && ClientUtils.isPlayerInGame()) {
			updateCooldown();
		}
    }

}
