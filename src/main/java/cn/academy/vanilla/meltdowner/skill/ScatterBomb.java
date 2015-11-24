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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.vanilla.meltdowner.entity.EntityMdBall;
import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class ScatterBomb extends Skill {

	public static final ScatterBomb instance = new ScatterBomb();

	private ScatterBomb() {
		super("scatter_bomb", 2);
	}
	
	static float getDamage(AbilityData aData) {
		return instance.callFloatWithExp("damage", aData);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new SBAction());
	};
	
	public static class SBAction extends SkillSyncAction {
		
		List<EntityMdBall> balls = new ArrayList();
		
		static IEntitySelector basicSelector = EntitySelectors.everything;
		static final int MAX_TICKS = 80, MOD = 10;
		static final double RAY_RANGE = 15;
		int ticks;

		public SBAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			super.onStart();
			cpData.perform(instance.getOverload(aData), 0);
		}
		
		@Override
		public void onTick() {
			ticks++;
			
			if(!isRemote) {
				if(ticks <= 80) {
					if(ticks >= 20 && ticks % MOD == 0) {
						EntityMdBall ball = new EntityMdBall(player);
						world.spawnEntityInWorld(ball);
						balls.add(ball);
					}
					
					if(!cpData.perform(0, instance.getConsumption(aData)))
						ActionManager.endAction(this);
				}
				
				if(ticks == 200) {
					player.attackEntityFrom(DamageSource.causePlayerDamage(player), 6);
					ActionManager.abortAction(this);
				}
			}
		}
		
		// Synchronize ball list
		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setInteger("c", balls.size());
			for(int i = 0; i < balls.size(); ++i) {
				tag.setInteger("" + i, balls.get(i).getEntityId());
			}
		}
		
		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			int n = tag.getInteger("c");
			while(n-- > 0) {
				Entity e = world.getEntityByID(tag.getInteger("" + n));
				if(e instanceof EntityMdBall)
					balls.add((EntityMdBall) e);
			}
		}
		
		@Override
		public void onEnd() {
			if(isRemote) {
				burstRaysClient();
			} else {
				for(EntityMdBall ball : balls) {
					Vec3 dest = newDest();
					MovingObjectPosition traceResult = Raytrace.perform(world,
						VecUtils.vec(ball.posX, ball.posY, ball.posZ), dest,
						EntitySelectors.and(basicSelector, EntitySelectors.excludeOf(player)));
					if(traceResult != null && traceResult.entityHit != null) {
						traceResult.entityHit.hurtResistantTime = -1;
						MDDamageHelper.attack(traceResult.entityHit, player, getDamage(aData));
					}
				}
				aData.addSkillExp(instance, instance.getFloat("expincr") * balls.size());
			}
		}
		
		@SideOnly(Side.CLIENT)
		private void burstRaysClient() {
			double yoff = ACRenderingHelper.isThePlayer(player) ? 0 : 1.6;
			for(EntityMdBall ball : balls) {
				// Spawn a ray for the ball
				EntityMdRaySmall raySmall = new EntityMdRaySmall(world);
				raySmall.viewOptimize = false;
				Vec3 dest = newDest();
				raySmall.setFromTo(ball.posX, ball.posY + yoff, ball.posZ,
					dest.xCoord, dest.yCoord, dest.zCoord);
				world.spawnEntityInWorld(raySmall);
			}
		}
		
		Vec3 newDest() {
			return new Motion3D(player, 5, true).move(RAY_RANGE).getPosVec();
		}
		
		@Override
		public void onFinalize() {
			if(!isRemote) {
				for(EntityMdBall e : balls)
					e.setDead();
			}
		}
		
	}

}
