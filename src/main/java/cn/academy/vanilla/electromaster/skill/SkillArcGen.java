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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.CPData;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.core.util.ACSounds;
import cn.academy.test.arc.EntityArc;
import cn.liutils.entityx.handlers.Life;
import cn.liutils.util.raytrace.Raytrace;

/**
 * @author WeAthFolD
 *
 */
public class SkillArcGen extends Skill {
	
	static SkillArcGen instance;

	public SkillArcGen() {
		super("arc_gen", 1);
		instance = this;
	}
	
	@Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant().addExecution(new ArcGenAction());
	}
	
	private static float getDamage(AbilityData data) {
		return instance.getFunc("damage").callFloat(data.getSkillExp(instance));
	}
	
	private static float getOverload(AbilityData data) {
		return instance.getFunc("overload").callFloat(data.getSkillExp(instance));
	}
	
	private static float getConsumption(AbilityData data) {
		return instance.getFunc("consumption").callFloat(data.getSkillExp(instance));
	}
	
	private static double getIgniteProb(AbilityData data) {
		return instance.getFunc("p_ignite").callFloat(data.getSkillExp(instance));
	}
	
	private static float getExpIncr(AbilityData data, boolean effectiveHit) {
		return instance.getFunc("exp_incr" + (effectiveHit ? "effective" : "ineffective")).callFloat(data.getSkillExp(instance));
	}
	
	public static class ArcGenAction extends SyncActionInstant {

		@Override
		public boolean validate() {
			AbilityData aData = AbilityData.get(player);
			CPData cpData = CPData.get(player);
			
			return cpData.perform(getOverload(aData), getConsumption(aData));
		}

		@Override
		public void execute() {
			AbilityData aData = AbilityData.get(player);
			
			if(!isRemote) {
				// Perform ray trace
				MovingObjectPosition result = Raytrace.traceLiving(player, 20);

				if(result != null && result.entityHit != null) {
					result.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), getDamage(aData));
				}
			} else {
				EntityArc arc = new EntityArc(player);
				arc.addMotionHandler(new Life(20));
				
				player.worldObj.spawnEntityInWorld(arc);
				ACSounds.playClient(player, "em.arc_weak", 0.5f, 1f);
			}
		}
		
	}

}
