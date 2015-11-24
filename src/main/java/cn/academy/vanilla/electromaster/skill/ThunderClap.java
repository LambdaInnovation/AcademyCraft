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
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.util.DamageHelper;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc.ArcType;
import cn.academy.vanilla.generic.entity.EntityRippleMark;
import cn.lambdalib.util.entityx.EntityCallback;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;

/**
 * @author WeAthFolD
 */
public class ThunderClap extends Skill {
	
	static final int MIN_TICKS = 40, MAX_TICKS = 60;
	
	public static final ThunderClap instance = new ThunderClap();

	private ThunderClap() {
		super("thunder_clap", 5);
	}
	
	private static float getDamage(AbilityData data, int ticks) {
		return instance.getFunc("damage").callFloat(data.getSkillExp(instance), ticks);
	}
	
	private static float getRange(AbilityData data) {
		return instance.callFloatWithExp("range", data);
	}
	
	private static int getCooldown(AbilityData data, int ticks) {
		return instance.getFunc("cooldown").callInteger(data.getSkillExp(instance), ticks);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new ThunderClapAction());
	}
	
	public static class ThunderClapAction extends SkillSyncAction {
		
		int ticks;
		double hitX, hitY, hitZ;

		public ThunderClapAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			if(isRemote)
				startEffects();
			
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
			cpData.perform(instance.getOverload(aData), 0);
		}
		
		@Override
		public void onTick() {
			if(isRemote)
				updateEffects();

			final double DISTANCE = 40.0;
			MovingObjectPosition pos = Raytrace.traceLiving(player, 40.0, EntitySelectors.nothing);
			if(pos != null) {
				hitX = pos.hitVec.xCoord;
				hitY = pos.hitVec.yCoord;
				hitZ = pos.hitVec.zCoord;
			} else {
				Motion3D mo = new Motion3D(player, true).move(DISTANCE);
				hitX = mo.px;
				hitY = mo.py;
				hitZ = mo.pz;
			}

			ticks++;
			if(ticks <= MIN_TICKS && !cpData.perform(0, instance.getConsumption(aData)))
				ActionManager.abortAction(this);
			if(!isRemote) {
				if(ticks >= MAX_TICKS) {
					ActionManager.endAction(this);
				}
			}
		}
		
		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setByte("t", (byte) ticks);
			tag.setFloat("x", (float) hitX);
			tag.setFloat("y", (float) hitY);
			tag.setFloat("z", (float) hitZ);
		}
		
		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			ticks = tag.getByte("t");
			hitX = tag.getFloat("x");
			hitY = tag.getFloat("y");
			hitZ = tag.getFloat("z");
		}
		
		@Override
		public void onEnd() {
			if(ticks < MIN_TICKS) {
				onAbort();
				return;
			}
			
			if(isRemote)
				endEffects();
			
			EntityLightningBolt lightning = new EntityLightningBolt(
					player.worldObj, hitX, hitY, hitZ);
			player.worldObj.spawnEntityInWorld(lightning);
			if(!isRemote) {
				DamageHelper.applyRangeAttack(player.worldObj, 
					hitX, hitY, hitZ, getRange(aData), getDamage(aData, ticks), 
					DamageSource.causePlayerDamage(player), EntitySelectors.excludeOf(player));
			}
			
			setCooldown(instance, getCooldown(aData, ticks));
			aData.addSkillExp(instance, instance.getFloat("expincr"));
			instance.triggerAchievement(player);
		}
		
		@Override
		public void onAbort() {
			if(isRemote)
				endEffects();
		}
		
		//CLIENT
		@SideOnly(Side.CLIENT)
		EntitySurroundArc surroundArc;
		
		@SideOnly(Side.CLIENT)
		EntityRippleMark mark;
		
		@SideOnly(Side.CLIENT)
		private void startEffects() {
			surroundArc = new EntitySurroundArc(player).setArcType(ArcType.BOLD);
			player.worldObj.spawnEntityInWorld(surroundArc);
			
			if(isLocal()) {
				world.spawnEntityInWorld(mark = new EntityRippleMark(world));
				mark.color.setColor4d(0.8, 0.8, 0.8, 0.7);
				mark.setPosition(hitX, hitY, hitZ);
			}
		}
		
		@SideOnly(Side.CLIENT)
		private void updateEffects() {
			if(isLocal()) {
				final float max = 0.1f, min = 0.001f;
				player.capabilities.setPlayerWalkSpeed(Math.max(min, max - (max - min) / 60 * ticks));
				mark.setPosition(hitX, hitY, hitZ);
			}
		}
		
		@SideOnly(Side.CLIENT)
		private void endEffects() {
			player.capabilities.setPlayerWalkSpeed(0.1f);
			if(surroundArc != null)
				surroundArc.executeAfter(new EntityCallback() {
					@Override
					public void execute(Entity target) {
						target.setDead();
					}
				}, 10);
			
			if(isLocal()) {
				mark.setDead();
			}
		}
		
	}

}
