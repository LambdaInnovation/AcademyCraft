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
package cn.academy.ability.electro.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import cn.academy.ability.electro.CatElectro;
import cn.academy.api.data.AbilityData;
import cn.liutils.api.entityx.FakeEntity;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.util.EntityUtils;
import cn.liutils.util.GenericUtils;

/**
 * @author WeathFolD
 */
public class EntityLF extends FakeEntity {
	
	static final int ATTACK_RATE = 2;
	int atkTicker;
	
	final AbilityData data;
	final float dmg;

	public EntityLF(AbilityData _data, int time) {
		super(_data.getPlayer());
		data = _data; 
		dmg = (float) GenericUtils.randIntv(14.0 + data.getSkillLevel(CatElectro.lightningFlash), 14 + data.getSkillLevel(CatElectro.lightningFlash) * 3);
		addDaemonHandler(new LifeTime(this, time));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(++atkTicker == ATTACK_RATE) {
			atkTicker = 0;
			EntityLivingBase elb = (EntityLivingBase) EntityUtils.getNearestEntityTo(this, 2, GenericUtils.selectorLiving, data.getPlayer());
			if(elb != null) {
				elb.attackEntityFrom(DamageSource.causePlayerDamage(data.getPlayer()), dmg);
			}
		}
	}

}
