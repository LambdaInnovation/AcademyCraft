/**
 * 
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
