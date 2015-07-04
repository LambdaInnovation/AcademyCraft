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

import net.minecraft.util.MovingObjectPosition;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Current charging/电流回充
 * @author WeAthFolD
 */
public class CurrentCharging extends Skill {

	static CurrentCharging instance;
	
	public CurrentCharging() {
		super("charging", 2);
		instance = this;
	}
	
	@SideOnly(Side.CLIENT)
	public SkillInstance createSkillInstance() {
		return new SkillInstanceInstant() {
			@Override
			public void execute() {
				
			}
		};
	}
	
	public static float getChargingSpeed(AbilityData data) {
		return call("speed", data);
	}
	
	public static float getConsumption(AbilityData data) {
		return call("consumption", data);
	}
	
	public static float getOverload(AbilityData data) {
		return call("overload", data);
	}
	
	public static float getExpIncr(AbilityData data, boolean effective) {
		return call("exp_incr_" + (effective ? "effective" : "ineffective"), data);
	}
	
	private static float call(String name, AbilityData data) {
		return instance.getFunc(name).callFloat(data.getSkillExp(instance));
	}
	
	public static class ActionChargeBlock extends SyncAction {

		protected ActionChargeBlock() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			
		}
		
		@Override
		public void onTick() {
			// Perform raytrace 
		}
		
		@Override
		public void onEnd() {
			endEffects();
		}
		
		@Override
		public void onAbort() {
			endEffects();
		}
		
		//CLIENT
		@SideOnly(Side.CLIENT)
		EntityArc arc;
		
		@SideOnly(Side.CLIENT)
		private void startEffects() {
			
		}
		
		@SideOnly(Side.CLIENT)
		private void updateEffects(MovingObjectPosition res, boolean isGood) {
			
		}
		
		@SideOnly(Side.CLIENT)
		private void endEffects() {
			
		}
		
	}
	
	public static class ActionChargeItem extends SyncAction {

		protected ActionChargeItem() {
			super(-1);
		}
		
	}

}
