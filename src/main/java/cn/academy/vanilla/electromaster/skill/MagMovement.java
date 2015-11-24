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

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.vanilla.electromaster.CatElectromaster;
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class MagMovement extends Skill {
	
	final static double ACCEL = 0.08d;

	static final String SOUND = "em.move_loop";
	public static final MagMovement instance = new MagMovement();

	private MagMovement() {
		super("mag_movement", 2);
	}
	
	private static double getMaxDistance(AbilityData data) {
		return 25;
	}
	
	private static double getVelocity(AbilityData data) {
		return 1;
	}
	
	private static float getExpIncr(double distance) {
		return instance.getFunc("expincr").callFloat(distance);
	}
	
	private static Target toTarget(AbilityData aData, World world, MovingObjectPosition pos) {
		if(pos.typeOfHit == MovingObjectType.BLOCK) {
			Block block = world.getBlock(pos.blockX, pos.blockY, pos.blockZ);
			if(aData.getSkillExp(instance) < 0.6f && !CatElectromaster.isMetalBlock(block))
				return null;
			if(!CatElectromaster.isWeakMetalBlock(block) && !CatElectromaster.isMetalBlock(block))
				return null;
			return new PointTarget(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord);
		} else {
			if(CatElectromaster.isEntityMetallic(pos.entityHit))
				return new EntityTarget(pos.entityHit);
			else
				return null;
		}
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		// Client ray-trace
		AbilityData aData = AbilityData.get(player);
		MovingObjectPosition result = Raytrace.traceLiving(player, getMaxDistance(aData));
		
		if(result != null) {
			World world = player.worldObj;
			
			Target t = toTarget(aData, player.worldObj, result);
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
			super(-1);
			target = _target;
		}

		public MovementAction() {
			super(-1);
		}
		
		@Override
		public void writeNBTStart(NBTTagCompound tag) {
			writeTarget(tag);
		}
		
		@Override
		public void readNBTStart(NBTTagCompound tag) {
			if(!isLocal())
				readTarget(tag);
		}
		
		private void writeTarget(NBTTagCompound tag) {
			tag.setDouble("x", target.x);
			tag.setDouble("y", target.y);
			tag.setDouble("z", target.z);
		}	
		
		private void readTarget(NBTTagCompound tag) {
			target = new DummyTarget(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
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
			} else {
				if((target != null && !target.alive()) || 
					!cpData.perform(instance.getOverload(aData), instance.getConsumption(aData)))
					ActionManager.abortAction(this);
			}
					
		}
		
		@Override
		public void onEnd() {
			if(!isRemote) {
				double traveledDistance = MathUtils.distance(sx, sy, sz, player.posX, player.posY, player.posZ);
				AbilityData aData = AbilityData.get(player);
				aData.addSkillExp(instance, getExpIncr(traveledDistance));
				instance.triggerAchievement(player);
			}
		}
		
		@Override
		public void onFinalize() {
			if(isRemote)
				endEffect();
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
			arc.setFromTo(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ, target.x, target.y, target.z);
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
	
	private static class DummyTarget extends Target {

		public DummyTarget(double _x, double _y, double _z) {
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

}
