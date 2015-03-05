/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
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
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityBomb;
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
public class SkillBomb extends SkillBase {

	public SkillBomb() {
		this.setLogo("meltdowner/bomb.png");
		this.setName("md_bomb");
		setMaxLevel(10);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new BombState(player);
			}
			
		}.setCooldown(0));
	}
	
	private static float getConsume(int slv, int lv) {
		return 150 + slv * 15 + lv * 10;
	}
	
	private static float getDamage(int slv, int lv) {
		return (float) GenericUtils.randIntv(slv * .3 + lv * .3, slv * .5 + lv * .8);
	}
	
	public static class BombState extends PatternHold.State {

		public BombState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			AbilityData data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(CatMeltDowner.bomb), lv = data.getLevelID() + 1;
			if(data.decreaseCP(getConsume(slv, lv))) {
			
				if(!isRemote()) {
					player.worldObj.spawnEntityInWorld(new EntityBomb(player, getDamage(slv, lv)));
				}
			}
		}

		@Override
		public boolean onFinish(boolean fin) {
			return true;
		}

		@Override
		public void onHold() {}
		
	}

}
