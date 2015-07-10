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
package cn.academy.vanilla.electromaster.skill;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class MagMovement extends Skill {
	
	final static double ACCEL = 0.08d;

	static final String SOUND = "em.move_loop";
	private static MagMovement instance;

	public MagMovement() {
		super("mag_movement", 2);
		instance = this;
	}
	
	private static double getMaxDistance(AbilityData data) {
		return 25;
	}
	
	private static double getVelocity(AbilityData data) {
		return 1;
	}
	
	private static float getExpIncr(double distance) {
		return instance.getFunc("exp_incr").callFloat(distance);
	}
	
	private static Target toTarget(MovingObjectPosition pos) {
		if(pos.typeOfHit == MovingObjectType.BLOCK) {
			return new PointTarget(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord);
		} else {
			return new EntityTarget(pos.entityHit);
		}
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		// Client ray-trace
		AbilityData aData = AbilityData.get(player);
		MovingObjectPosition result = Raytrace.traceLiving(player, getMaxDistance(aData));
		
		if(result != null) {
			Target t = toTarget(result);
			if(t != null) {
				return new SkillInstance().addChild(new MovementAction(t));
			}
		}
		return null;
	}
	
	public static class MovementAction extends SyncAction {
		
		double mox, moy, moz;
		
		double sx, sy, sz;
		
		Target target;
		
		public MovementAction(Target _target) {
			super(5);
			target = _target;
		}

		public MovementAction() {
			super(5);
		}
		
		@Override
		public void onStart() {
			if(isRemote) {
				startEffect();
			} else {
				sx = player.posX;
				sy = player.posY;
				sz = player.posZ;
			}
		}
		
		@Override
		public void onTick() {
			AbilityData aData = AbilityData.get(player);
			CPData cpData = CPData.get(player);
			
			if(isLocal()) {
				target.tick();
				
				double 
					dx = target.x - player.posX,
					dy = target.y - player.posY,
					dz = target.z - player.posZ;
			
				double lastMo = MathUtils.lengthSq(player.motionX, player.motionY, player.motionZ);
				if(Math.abs(MathUtils.lengthSq(mox, moy, moz) - lastMo) > 0.5) {
					mox = player.motionX;
					moy = player.motionY;
					moz = player.motionZ;
				}
				
				double mod = Math.sqrt(dx * dx + dy * dy + dz * dz) / getVelocity(aData);
				
				dx /= mod; dy /= mod; dz /= mod;
				
				mox = player.motionX = tryAdjust(mox, dx);
				moy = player.motionY = tryAdjust(moy, dy);
				moz = player.motionZ = tryAdjust(moz, dz);
			}
			
			if(isRemote) {
				updateEffect();
			}
			
			if((target != null && !target.alive()) || !cpData.perform(
					instance.getOverload(aData), instance.getConsumption(aData)))
				ActionManager.abortAction(this);
		}
		
		@Override
		public void onAbort() {
			if(isRemote) endEffect();
		}
		
		@Override
		public void onEnd() {
			if(isRemote) {
				endEffect();
			} else {
				double traveledDistance = MathUtils.distance(sx, sy, sz, player.posX, player.posY, player.posZ);
				AbilityData aData = AbilityData.get(player);
				aData.addSkillExp(instance, getExpIncr(traveledDistance));
			}
		}
		
		@SideOnly(Side.CLIENT)
		private EntityArc arc;
		
		@SideOnly(Side.CLIENT)
		private FollowEntitySound sound;
		
		@SideOnly(Side.CLIENT)
		private void startEffect() {
			arc = new EntityArc(player, ArcPatterns.thinContiniousArc);
			arc.lengthFixed = false;
			arc.texWiggle = 1;
			arc.showWiggle = 0.1;
			arc.hideWiggle = 0.6;
			
			player.worldObj.spawnEntityInWorld(arc);
			
			ACSounds.playClient(sound = new FollowEntitySound(player, SOUND).setLoop());
		}
		
		@SideOnly(Side.CLIENT)
		private void updateEffect() {
			arc.setFromTo(player.posX, player.posY, player.posZ, target.x, target.y, target.z);
		}
		
		@SideOnly(Side.CLIENT)
		private void endEffect() {
			arc.setDead();
			sound.stop();
		}
		
		private double tryAdjust(double from, double to) {
			double d = to - from;
			if(Math.abs(d) < ACCEL) {
				return to;
			}
			return d > 0 ? from + ACCEL : from - ACCEL;
		}
		
	}
	
	private static abstract class Target {
		
		double x, y, z;
		
		abstract void tick();
		
		abstract boolean alive();
		
	}
	
	private static class PointTarget extends Target {
		
		public PointTarget(double _x, double _y, double _z) {
			x = _x;
			y = _y;
			z = _z;
		}

		@Override
		void tick() {}

		@Override
		boolean alive() {
			return true;
		}
		
	}
	
	private static class EntityTarget extends Target {
		
		final Entity target;
		
		public EntityTarget(Entity _t) {
			target = _t;
		}
		
		@Override
		void tick() {
			x = target.posX;
			y = target.posY + target.getEyeHeight();
			z = target.posZ;
		}

		@Override
		boolean alive() {
			return !target.isDead;
		}
		
	}

}
