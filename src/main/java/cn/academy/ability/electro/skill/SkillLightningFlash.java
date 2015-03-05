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
package cn.academy.ability.electro.skill;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.skill.ChargePlaneEffect;
import cn.academy.ability.electro.entity.EntityLF;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.render.SkillRenderManager;
import cn.liutils.util.GenericUtils;

/**
 * 电光一闪技能
 * @author WeathFolD
 */
public class SkillLightningFlash extends SkillBase {
	
	Random rand = new Random();

	public SkillLightningFlash() {
		this.setLogo("electro/lf.png");
		setName("em_lf");
		setMaxLevel(5);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new LFState(player);
			}
			
		}.setCooldown(30000));
	}
	
	public static class LFState extends PatternHold.State {

		public LFState(EntityPlayer player) {
			super(player);
		}
		
		@Override
		public void onStart() {
			AbilityData data = AbilityDataMain.getData(player);
			
			int slv = data.getSkillLevel(CatElectro.lightningFlash);
			int time = 100 + slv * 10;
			int ccp = 2200 - 20 * (slv * slv);
			float dmg = (float) GenericUtils.randIntv((double)5, 5 + slv * 2);
			
			if(!data.decreaseCP(ccp, CatElectro.lightningFlash)) {
				return;
			}
			
			//give buff
			if(!player.worldObj.isRemote) {
				player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, time, 4));
				player.addPotionEffect(new PotionEffect(Potion.jump.id, time, 4));
				player.worldObj.spawnEntityInWorld(new EntityLF(data, time));
			} else {
				player.worldObj.spawnEntityInWorld(new EntityLF(data, time));
				player.worldObj.spawnEntityInWorld(new ChargeEffectS.Strong(player, time, 3));
				SkillRenderManager.addEffect(new ChargePlaneEffect(), time * 50);
			}
		}

		@Override
		public boolean onFinish(boolean fin) { return true; }

		@Override
		public void onHold() {}
		
	}

}
