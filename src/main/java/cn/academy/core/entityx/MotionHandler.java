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
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public abstract class MotionHandler<T extends Entity> {

	public boolean isActive = true;
	
	boolean isDead = false;
	
	/**
	 * The field is set by EntityX when added into it.
	 */
	T target;
	/**
	 * The field is set by EntityX when added into it.
	 */
	EntityX entityX;
	
	public MotionHandler() {}
	
	public abstract String getID();
	
	public abstract void onStart();
	
	/**
	 * OnUpdate events will only be sent as long as isActive set to true
	 */
	public abstract void onUpdate();
	
	public void setDead() {
		isDead = true;
	}
	
	protected T getTarget() {
		return target;
	}
	
	protected EntityX getEntityX() {
		return entityX;
	}
	
	protected World world() {
		return target.worldObj;
	}
	
	public boolean isRemote() {
		return target.worldObj.isRemote;
	}
	
}
