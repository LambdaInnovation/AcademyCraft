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
package cn.academy.ability.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.meltdowner.entity.EntityMiningRayBase;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.ClientUtils;

/**
 * @author WeathFolD
 *
 */
public abstract class SkillMiningBase extends SkillBase {
	
	static final int LOOP_TIME = 55;
	
	ResourceLocation loopSrc;
	String loopName;
	float pitch;
	
	public SkillMiningBase(float _pitch) {
		pitch = _pitch;
		loopName = "academy:md.mine_simple";
		loopSrc = new ResourceLocation(loopName);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new MiningState(player, SkillMiningBase.this);
			}
			
		}.setCooldown(0));
	}
	
	abstract float getConsume(int slv, int lv); //per tick
	
	protected abstract EntityMiningRayBase createEntity(AbilityData data);
	
	public static class MiningState extends State {
		
		int slv, lv;
		float ccp;
		int harvLevel, spawnRate;
		SkillMiningBase instance;
		AbilityData data;
		EntityMiningRayBase ray;
		
		public MiningState(EntityPlayer player, SkillMiningBase _instance) {
			super(player);
			instance = _instance;
			data = AbilityDataMain.getData(player);
			slv = data.getSkillLevel(instance);
			lv = data.getLevelID() + 1;
			ccp = instance.getConsume(slv, lv);
			ray = instance.createEntity(data);
		}

		public MiningState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if(!isRemote()) {
				player.worldObj.spawnEntityInWorld(ray);
			}
		}

		@Override
		public boolean onFinish(boolean ended) {
			if(!isRemote()) {
				ray.setDead();
			}
			return true;
		}

		@Override
		public void onHold() {}
		
		@Override
		public boolean onTick(int ticks) {
			if((ticks - 1) % LOOP_TIME == 0) {
				if(isRemote()) {
					ClientUtils.playSound(instance.loopSrc, instance.pitch);
				} else {
					player.playSound(instance.loopName, 1.0f, instance.pitch);
				}
			}
			
			if(isRemote())
				return false;
			return !data.decreaseCP(ccp, instance);
		}
		
	}

}
