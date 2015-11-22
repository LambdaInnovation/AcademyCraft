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

import static cn.lambdalib.util.generic.RandUtils.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityMdBall;
import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cn.lambdalib.networkcall.s11n.StorageOption.RangedTarget;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@Registrant
public class ElectronMissile extends Skill {
	
	public static final ElectronMissile instance = new ElectronMissile();
	
	static int MAX_HOLD = 5;

	private ElectronMissile() {
		super("electron_missile", 5);
	}

	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new EMAction());
	}
	
	public static class EMAction extends SkillSyncAction {
		
		LinkedList<EntityMdBall> active;
		
		int ticks;
		
		@Override
		public void onStart() {
			super.onStart();
			
			if(!isRemote) {
				active = new LinkedList();
			}
		}
		
		@Override
		public void onTick() {
			if(!cpData.perform(instance.getOverload(aData), instance.getConsumption(aData)) && !isRemote)
				ActionManager.abortAction(this);
			
			if(!isRemote) {
				if(ticks > instance.callFloatWithExp("time_limit", aData))
					ActionManager.abortAction(this);
				
				if(ticks % 10 == 0) {
					if(active.size() < MAX_HOLD) {
						EntityMdBall ball = new EntityMdBall(player);
						world.spawnEntityInWorld(ball);
						active.add(ball);
					}
				}
				if(ticks != 0 && ticks % 8 == 0) {
					List<Entity> list = WorldUtils.getEntities(player, instance.callFloatWithExp("range", aData), 
						EntitySelectors.and(EntitySelectors.excludeOf(player), EntitySelectors.living));
					if(!active.isEmpty() && !list.isEmpty() && cpData.perform(
						instance.callFloatWithExp("overload_attacked", aData), 
						instance.callFloatWithExp("consumption_attacked", aData))) {
						double min = Double.MAX_VALUE;
						Entity result = null;
						for(Entity e : list) {
							double dist = e.getDistanceToEntity(player);
							if(dist < min) {
								min = dist;
								result = e;
							}
						}
						
						// Find a ramdom ball and destroy it
						int index = 1 + nextInt(active.size());
						Iterator<EntityMdBall> iter = active.iterator();
						EntityMdBall ball = null;
						while(index --> 0)
							ball = iter.next();
						iter.remove();
						
						// client action
						spawnRay(player, world, 
							VecUtils.entityPos(ball), 
							VecUtils.add(VecUtils.entityPos(result), VecUtils.vec(0, result.getEyeHeight(), 0)));
						
						// server action
						result.hurtResistantTime = -1;
						MDDamageHelper.attack(result, player, instance.callFloatWithExp("damage", aData));
						aData.addSkillExp(instance, instance.getFloat("expincr"));
						ball.setDead();
					}
				}
			} else
				updateEffect();
			
			++ticks;
		}
		
		@Override
		public void onEnd() {
			setCooldown(instance, instance.getFunc("cooldown").callInteger(ticks));
		}
		
		@Override
		public void onFinalize() {
			if(!isRemote) {
				for(EntityMdBall ball : active) {
					ball.setDead();
				}
			}
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		void updateEffect() {
			int count = rangei(1, 3);
			while(count --> 0) {
				double r = ranged(0.5, 1);
				double theta = ranged(0, Math.PI * 2);
				double h = ranged(-1.2, 0);
				Vec3 pos = VecUtils.add(VecUtils.vec(player.posX, player.posY + ACRenderingHelper.getHeightFix(player), player.posZ), VecUtils.vec(r * Math.sin(theta), h, r * Math.cos(theta)));
				Vec3 vel = VecUtils.vec(ranged(-.02, .02), ranged(.01, .05), ranged(-.02, .02));
				world.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(world, pos, vel));
			}
		}
		
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void spawnRay(@RangedTarget(range = 15) EntityPlayer _player, @Instance World world, @Data Vec3 from, @Data Vec3 to) {
		jobSpawnRay(world, from, to);
	}
	
	@SideOnly(Side.CLIENT)
	static void jobSpawnRay(World world, Vec3 from, Vec3 to) {
		EntityMdRaySmall ray = new EntityMdRaySmall(world);
		ray.setFromTo(from, to);
		world.spawnEntityInWorld(ray);
	}
	
}
