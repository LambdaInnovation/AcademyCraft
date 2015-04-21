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

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Hardcoded with an EntityX. Use this for fast implementation.
 * @author WeAthFolD
 */
public abstract class EntityAdvanced extends Entity {
	
	public EntityX entityX;

	public EntityAdvanced(World world) {
		super(world);
		entityX = new EntityX(this);
	}

	@Override
	protected void entityInit() {}

	private boolean firstUpdate = true;
	@Override
	public void onUpdate() {
		if(firstUpdate) {
			firstUpdate = false;
			entityX.startUpdate();
			onFirstUpdate();
		}
		entityX.update();
	}
	
	protected void onFirstUpdate() {
		
	}
	
	/**
	 * Designed for prototype pattern.
	 */
	public void reset() {
		firstUpdate = true;
	}
	
	public void addMotionHandler(MotionHandler mh) {
		entityX.addMotionHandler(mh);
	}
	
	public void execute(EntityCallback c) {
		entityX.execute(c);
	}
	
	public void executeAfter(EntityCallback c, int ticks) {
		entityX.executeAfter(c, ticks);
	}
	
	public void postEvent(EntityEvent event) {
		entityX.postEvent(event);
	}
	
	public void regEventHandler(EntityEventHandler eeh) {
		entityX.regEventHandler(eeh);
	}

	public void resetEntityX() {
		entityX = new EntityX(this);
	}
	
}
