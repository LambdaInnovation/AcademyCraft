/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMeltDowner;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.GenericUtils;

/**
 * @author WeathFolD
 *
 */
public class SkillMeltDowner extends SkillBase {

	public SkillMeltDowner() {
		this.setLogo("meltdowner/meltdown.png");
		this.setName("md_meltdown");
		setMaxLevel(6);
	}
	
	private static float getCPConsume(int slv, int lv) {
		return 10 + slv * 1.2f + lv * 0.6f;
	}
	
	private static float getDamage(int slv, int lv, int ticks) {
		return (ticks * 0.025f) * (float)(10 + GenericUtils.randIntv(slv + lv * 1.2, slv * 1.2 + lv * 1.8));
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(120) {
			@Override
			public State createSkill(EntityPlayer player) {
				return new MDState(player);
			}
		}.setCooldown(3000));
	}
	
	public static class MDState extends PatternHold.State {
		
		//TODO: Current used built-in abort logic. Change when API update.
		final AbilityData data;
		boolean spawn = true;
		
		EntityMeltDowner mdRay;
		
		final float ccp;

		public MDState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(CatMeltDowner.meltDowner), lv = data.getLevelID() + 1;
			ccp = getCPConsume(slv, lv);
		}

		@Override
		public void onStart() {
			//if(!isRemote())
		}

		@Override
		public boolean onFinish(boolean res) {
			if(!spawn || this.getTickTime() < 12 || !res) {
				return false;
			}
			
			if(!isRemote()) {
				int slv = data.getSkillLevel(CatMeltDowner.meltDowner), lv = data.getLevelID() + 1;
				int rt = Math.min(getTickTime(), 40);
				player.worldObj.spawnEntityInWorld(new EntityMeltDowner(player, getDamage(slv, lv, rt)));
			}
			return true;
		}
		
		@Override
		public boolean onTick(int ticks) {
			if(!data.decreaseCP(ccp, CatMeltDowner.meltDowner)) {
				spawn = false;
				return true;
			}
			if(ticks == 120) {
				player.attackEntityFrom(DamageSource.causePlayerDamage(player), 
					Math.min(player.getHealth() - 0.1f, 5.0f));
				spawn = false;
				return true;
			}
			return false;
		}

		@Override
		public void onHold() {}
		
	}

}
