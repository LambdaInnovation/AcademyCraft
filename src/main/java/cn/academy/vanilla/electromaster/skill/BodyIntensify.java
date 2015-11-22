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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.vanilla.electromaster.client.effect.CurrentChargingHUD;
import cn.academy.vanilla.electromaster.entity.EntityIntensifyEffect;
import cn.lambdalib.util.client.auxgui.AuxGuiHandler;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Body Intensify/生物电强化
 * @author WeAthFolD
 */
public class BodyIntensify extends Skill {
	
	static final int MIN_TIME = 10, MAX_TIME = 40, MAX_TOLERANT_TIME = 100;
	static final String LOOP_SOUND = "em.intensify_loop", ACTIVATE_SOUND = "em.intensify_activate";
	
	static final List<PotionEffect> effects = new ArrayList();
	static {
		effects.add(new PotionEffect(Potion.moveSpeed.id, 0, 3));
		effects.add(new PotionEffect(Potion.jump.id, 0, 1));
		effects.add(new PotionEffect(Potion.regeneration.id, 0, 1));
		effects.add(new PotionEffect(Potion.damageBoost.id, 0, 1));
		effects.add(new PotionEffect(Potion.resistance.id, 0, 1));
	}
	
	public static final BodyIntensify instance = new BodyIntensify();
	
	private static PotionEffect createEffect(PotionEffect effect, int level, int duration) {
		return new PotionEffect(effect.getPotionID(), duration, Math.min(level, effect.getAmplifier()), effect.getIsAmbient());
	}

	private BodyIntensify() {
		super("body_intensify", 3);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new IntensifyAction());
	}
	
	// CT: ChargeTime
	
	private static double getProbability(int ct) {
		return instance.getFunc("probability").callDouble(ct);
	}
	
	private static int getBuffTime(AbilityData data, int ct) {
		return instance.getFunc("time").callInteger(data.getSkillExp(instance), ct);
	}
	
	private static int getHungerBuffTime(int ct) {
		return instance.getFunc("hunger_time").callInteger(ct);
	}
	
	private static int getBuffLevel(AbilityData data, int ct) {
		return instance.getFunc("level").callInteger(data.getSkillExp(instance), ct);
	}
	
	public static class IntensifyAction extends SkillSyncAction {
		
		int tick;
		float consumption;

		public IntensifyAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			super.onStart();
			consumption = instance.getConsumption(aData);
			
			cpData.perform(instance.getOverload(aData), 0);
			
			if(isRemote) 
				startEffect();
		}
		
		@Override
		public void onTick() {
			tick++;
			if(!isRemote && 
			   ((tick <= MAX_TIME && !cpData.perform(0, consumption)) || 
			     tick >= MAX_TOLERANT_TIME) ) {
				ActionManager.endAction(this);
			}
			
			if(isRemote)
				updateEffect();
		}
		
		// Synchronize tick to prevent corrupted logic.
		@Override
		public void readNBTFinal(NBTTagCompound tag) { 
			tick = tag.getInteger("t");
		}
		
		@Override
		public void writeNBTFinal(NBTTagCompound tag) { 
			tag.setInteger("t", tick);
		}
		
		@Override
		public void onEnd() {
			if(tick >= MIN_TIME) {
				if(tick >= MAX_TIME) tick = MAX_TIME;
				
				if(!isRemote) {
					Collections.shuffle(effects);
					
					double p = getProbability(tick);
					int i = 0;
					int time = getBuffTime(aData, tick);
					
					while(p > 0) {
						double a = RandUtils.ranged(0, 1);
						if(a < p) {
							// Spawn a new buff
							int level = getBuffLevel(aData, tick);
							player.addPotionEffect(createEffect(effects.get(i++), level, time));
						}
						
						p -= 1.0;
					}
					
					// Also give him a hunger buff
					player.addPotionEffect(new PotionEffect(Potion.hunger.id, getHungerBuffTime(tick), 2));
					instance.triggerAchievement(player);
				}
				
				aData.addSkillExp(instance, instance.getFloat("expincr"));
				setCooldown(instance, instance.getCooldown(aData));
				
				if(isRemote) 
					endEffect(true);
				
			} else {
				if(isRemote) 
					endEffect(false);
			}
		}
		
		@Override
		public void onAbort() {
			if(isRemote) 
				endEffect(false);
		}
		
		// CLIENT
		
		@SideOnly(Side.CLIENT)
		FollowEntitySound loopSound;
		
		@SideOnly(Side.CLIENT)
		CurrentChargingHUD hud;
		
		@SideOnly(Side.CLIENT)
		public void startEffect() {
			if(isLocal()) {
				ACSounds.playClient(loopSound = new FollowEntitySound(player, LOOP_SOUND).setLoop());
				
				AuxGuiHandler.register(hud = new CurrentChargingHUD());
			}
		}
		
		@SideOnly(Side.CLIENT)
		public void updateEffect() {
			// N/A
		}
		
		@SideOnly(Side.CLIENT)
		public void endEffect(boolean performed) {
			if(isLocal()) {
				if(loopSound != null)
					loopSound.stop();
				if(hud != null)
					hud.startBlend(performed);
			}
			
			if(performed) {
				ACSounds.playClient(player, ACTIVATE_SOUND, 0.5f);
				player.worldObj.spawnEntityInWorld(new EntityIntensifyEffect(player));
			}
		}
		
	}
	
}
