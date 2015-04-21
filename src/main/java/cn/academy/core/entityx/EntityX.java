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
package cn.academy.core.entityx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;

/**
 * Compared to the old API, this class is designed to be delegated. Entities should create EntityX by themselves and delegate event&calls.
 * This enables more flexible design.
 * @author WeAthFolD
 */
public class EntityX<T extends Entity> {
	
	Map<String, MotionHandler> motionHandlers = new HashMap();
	Map<Integer, List<EntityCallback>> callbacks = new HashMap();
	
	Map<Class<? extends EntityEvent>, List<EntityEventHandler>> eventHandlers = new HashMap();
	
	final T target;
	boolean updated = false;
	
	public EntityX(T _target) {
		target = _target;
	}
	
	public void startUpdate() {
		updated = true;
		for(MotionHandler mh : motionHandlers.values()) {
			mh.onStart();
		}
	}
	
	public void update() {
		Iterator<MotionHandler> iter = motionHandlers.values().iterator();
		while(iter.hasNext()) {
			MotionHandler mh = iter.next();
			if(mh.isDead) {
				iter.remove();
			} else if(mh.isActive) {
				mh.onUpdate();
			}
		}
		
		List<EntityCallback> ecList = callbacks.remove(target.ticksExisted);
		if(ecList != null) {
			for(EntityCallback ec : ecList) {
				ec.execute(target);
			}
		}
	}
	
	public void addMotionHandler(MotionHandler mh) {
		if(motionHandlers.containsKey(mh.getID()))
			throw new IllegalStateException("Duplicate MotionHandler ID");
		
		mh.target = target;
		mh.entityX = this;
		motionHandlers.put(mh.getID(), mh);
		if(updated) {
			mh.onStart();
		}
	}
	
	public <T extends MotionHandler> T getMotionHandler(String id) {
		T ret = (T) motionHandlers.get(id);
		return ret;
	}
	
	public void execute(EntityCallback c) {
		c.execute(target);
	}
	
	public void executeAfter(EntityCallback c, int ticks) {
		int targetTime = ticks + target.ticksExisted;
		List<EntityCallback> list = callbacks.get(targetTime);
		if(list == null) {
			list = new ArrayList();
			callbacks.put(targetTime, list);
		}
		list.add(c);
	}
	
	public void postEvent(EntityEvent event) {
		List<EntityEventHandler> list = eventHandlers.get(event.getClass());
		if(list != null) {
			for(EntityEventHandler eeh : list) {
				if(eeh.active)
					eeh.onEvent(event);
			}
		}
	}
	
	public void regEventHandler(EntityEventHandler eeh) {
		List<EntityEventHandler> list = eventHandlers.get(eeh.getHandledEvent());
		if(list == null) {
			list = new ArrayList();
			eventHandlers.put(eeh.getHandledEvent(), list);
		}
		list.add(eeh);
	}
	
}
