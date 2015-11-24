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
package cn.academy.vanilla.meltdowner.skill;

import static cn.lambdalib.util.generic.RandUtils.ranged;
import static cn.lambdalib.util.generic.RandUtils.rangei;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.core.util.RangedRayDamage;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityMDRay;
import cn.lambdalib.util.generic.VecUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

/**
 * @author WeAthFolD
 */
public class Meltdowner extends Skill {
	
	static final int TICKS_MIN = 20, TICKS_MAX = 40, TICKS_TOLE = 100;

	public static final Meltdowner instance = new Meltdowner();
	
	private Meltdowner() {
		super("meltdowner", 3);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new MDAction()).setEstmCP(
			instance.getConsumption(AbilityData.get(player)));
	}
	
	public static class MDAction extends SkillSyncAction {
		
		int ticks;

		public MDAction() {}
		
		@Override
		public void onStart() {
			super.onStart();
			
			if(isRemote) startEffect();
		}
		
		@Override
		public void onTick() {
			ticks++;
			if(isRemote)
				updateEffect();
			if(!cpData.perform(0, instance.getConsumption(aData)) && !isRemote)
				ActionManager.abortAction(this);
			
			if(ticks > TICKS_TOLE)
				ActionManager.abortAction(this);
		}
		
		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setInteger("t", ticks);
		}
		
		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			ticks = tag.getInteger("t");
		}
		
		@Override
		public void onEnd() {
			if(ticks < TICKS_MIN) {
				// N/A
			} else {
				cpData.perform(instance.getOverload(aData), 0);
				int ct = toChargeTicks();
				
				if(isRemote) {
					spawnRay();
				} else {
					RangedRayDamage rrd = new RangedRayDamage(player, 
						instance.getFunc("range").callFloat(aData.getSkillExp(instance)),
						instance.getFunc("energy").callFloat(aData.getSkillExp(instance), ct));
					rrd.startDamage = instance.getFunc("damage").callFloat(aData.getSkillExp(instance), ct);
					rrd.perform();
				}
				
				setCooldown(instance, instance.getFunc("cooldown").callInteger(aData.getSkillExp(instance), ct));
				aData.addSkillExp(instance, instance.getFunc("expincr").callFloat(ct));
			}
		}
		
		@Override
		public void onFinalize() {
			if(isRemote)
				endEffect();
		}
		
		private int toChargeTicks() {
			return Math.min(ticks, TICKS_MAX);
		}
		
		// CLIENT
		
		@SideOnly(Side.CLIENT)
		FollowEntitySound sound;
		
		@SideOnly(Side.CLIENT)
		void spawnRay() {
			EntityMDRay ray = new EntityMDRay(player);
			ACSounds.playClient(player, "md.meltdowner", 0.5f);
			world.spawnEntityInWorld(ray);
		}
		
		@SideOnly(Side.CLIENT)
		void startEffect() {
			sound = new FollowEntitySound(player, "md.md_charge").setVolume(1.0f);
			ACSounds.playClient(sound);
		}
		
		@SideOnly(Side.CLIENT)
		void updateEffect() {
			if(isLocal()) {
				player.capabilities.setPlayerWalkSpeed(0.1f - ticks * 0.001f);
			}
			
			// Particles surrounding player
			int count = rangei(2, 3);
			while(count --> 0) {
				double r = ranged(0.7, 1);
				double theta = ranged(0, Math.PI * 2);
				double h = ranged(-1.2, 0);
				Vec3 pos = VecUtils.add(VecUtils.vec(player.posX, 
					player.posY + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), player.posZ), 
					VecUtils.vec(r * Math.sin(theta), h, r * Math.cos(theta)));
				Vec3 vel = VecUtils.vec(ranged(-.03, .03), ranged(.01, .05), ranged(-.03, .03));
				world.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(world, pos, vel));
			}
		}
		
		@SideOnly(Side.CLIENT)
		void endEffect() {
			if(isLocal()) {
				player.capabilities.setPlayerWalkSpeed(0.1f);
			}
			
			sound.stop();
		}
		
	}

}
