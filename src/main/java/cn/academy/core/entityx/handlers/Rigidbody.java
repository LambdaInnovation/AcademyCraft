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
package cn.academy.core.entityx.handlers;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import cn.academy.core.entityx.MotionHandler;
import cn.academy.core.entityx.event.CollideEvent;
import cn.liutils.util.GenericUtils;

/**
 * Rigidbody will update velocity and apply gravity and do simple collision.
 * @author WeAthFolD
 */
public class Rigidbody extends MotionHandler {
	
	public double gravity = 0.00; //block/tick^2
	public IEntitySelector filter;

	@Override
	public String getID() {
		return "Rigidbody";
	}

	@Override
	public void onStart() {}

	@Override
	public void onUpdate() {
		Entity target = getTarget();
		
		//Collision detection
		Vec3 cur = Vec3.createVectorHelper(target.posX, target.posY, target.posZ),
			next = Vec3.createVectorHelper(target.posX + target.motionX, target.posY + target.motionY, target.posZ + target.motionZ);
		MovingObjectPosition mop = GenericUtils.rayTraceBlocksAndEntities(filter, world(), cur, next, target);
		if(mop != null) {
			getEntityX().postEvent(new CollideEvent(mop)); //Let the event handlers do the actual job.
		}
		
		//Velocity update
		target.motionY -= gravity;
		
		target.lastTickPosX = target.posX;
		target.lastTickPosY = target.posY;
		target.lastTickPosZ = target.posZ;
		target.setPosition(target.posX + target.motionX, target.posY + target.motionY, target.posZ + target.motionZ);
		
		System.out.println(target.motionY);
	}

}
