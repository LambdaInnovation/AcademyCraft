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
package cn.academy.vanilla.electromaster.skill.ironsand;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.SpecialSkill;
import cn.academy.ability.api.SubSkill;
import cn.academy.vanilla.electromaster.client.effect.IronSandParticles;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Iron sand manipulation
 * @author WeAthFolD
 */
public class IronSand extends SpecialSkill {

	static IronSand instance;
	
	public IronSand() {
		super("iron_sand", 4);
		
		instance = this;
		
		addSubSkill(new ISSword());
		addSubSkill(new ISWhip());
		addSubSkill(new ISStorm());
		addSubSkill(new ISCone());
	}
	
	/**
	 * Called when player started SpecialSkill. Validate at SERVER to proceed.
	 * @param player
	 * @return
	 */
	@Override
	public boolean validateExecution(EntityPlayer player) {
		return true;
	}
	
	@Override
	protected SpecialSkillAction getSpecialAction(EntityPlayer player) {
		return new IronSandAction();
	}
	
	public static class IronSandAction extends SpecialSkillAction {
		
		public IronSandAction() {
			super(instance, -1);
		}
		
		@Override
		protected void onSkillStart() {
			if(isRemote)
				startEffects();
		}
		
		@Override
		protected void onSkillTick() {
			if(isRemote)
				updateEffects();
		}
		
		@Override
		protected void onSkillEnd() {
			if(isRemote)
				endEffects();
		}
		
		@Override
		protected void onSkillAbort() {
			if(isRemote)
				endEffects();
		}
		
		// CLIENT ONLY, type API
		private String currentType = "idle";
		
		public void setCurrentType(String type) {
			endCurrentType();
			currentType = type;
			startCurrentType();
		}
		
		public String getCurrentType() {
			return currentType;
		}
		
		private void endCurrentType() {
			SubSkill ss = instance.getSubSkill(currentType);
			if(ss != null && ss instanceof ISStateCallback) {
				((ISStateCallback)ss).endState(player);
			}
		}
		
		private void startCurrentType() {
			SubSkill ss = instance.getSubSkill(currentType);
			if(ss != null && ss instanceof ISStateCallback) {
				((ISStateCallback)ss).startState(player);
			}
		}
				
		// ------------------
		
		// CLIENT EFFS
		
		@SideOnly(Side.CLIENT)
		IronSandParticles particles;
		
		@SideOnly(Side.CLIENT)
		private void startEffects() {
			particles = new IronSandParticles(player);
		}
		
		@SideOnly(Side.CLIENT)
		private void updateEffects() {
			particles.tick();
		}
		
		@SideOnly(Side.CLIENT)
		private void endEffects() {
		}
		
	}

}
