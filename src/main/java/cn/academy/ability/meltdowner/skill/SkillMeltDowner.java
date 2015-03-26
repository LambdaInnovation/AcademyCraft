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
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMeltDowner;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.GenericUtils;

/**
 * @author WeathFolD
 */
public class SkillMeltDowner extends SkillBase {

	public SkillMeltDowner() {
		this.setLogo("meltdowner/meltdown.png");
		this.setName("md_meltdown");
		setMaxLevel(6);
	}
	
	private static float getCPConsume(int slv, int lv) {
		return 120 * (10 + slv * 1.2f + lv * 0.6f);
	}
	
	private static float getDamage(int slv, int lv) {
		return (2f) * (float)(10 + GenericUtils.randIntv(slv + lv * 1.2, slv * 1.2 + lv * 1.8));
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new MDState(player);
			}
			
		}.setCooldown(3000));
	}
	
	public static class MDState extends SkillState {
		
		AbilityData data = AbilityDataMain.getData(player);
		int slv = data.getSkillLevel(CatMeltDowner.meltDowner), lv = data.getLevelID() + 1;
		
		final float max = 0.1f, min = 0.001f;

		public MDState(EntityPlayer player) {
			super(player);
		}
		
		@Override
		protected void onStart() {
			if(!isRemote()) {
				player.worldObj.playSoundAtEntity(player, "academy:md.md_charge", 0.5f, 1.0f);
			}
		}
		
		@Override
		protected boolean onTick(int ticks) {
		    if(isRemote()) {
		        player.capabilities.setPlayerWalkSpeed(Math.max(min, max - (max - min) / 100 * ticks));
		    }
		    
		    if(ticks == 120) {
		        if(!data.decreaseCP(getCPConsume(slv, lv), CatMeltDowner.meltDowner)) {
		            finishSkill(false);
		            return false;
		        }
		        if(!isRemote()) {
		            player.worldObj.playSoundAtEntity(player, "academy:md.meltdowner", 0.5f, 1.0f);
	                player.worldObj.spawnEntityInWorld(new EntityMeltDowner(player, getDamage(slv, lv)));
		        }
		        this.finishSkill(true);
		    }
		    if(!isRemote())
                player.capabilities.setPlayerWalkSpeed(.1f);
            return false;
		}
		
		@Override
		protected boolean onFinish(boolean res) {
		    if(isRemote())
		        player.capabilities.setPlayerWalkSpeed(.1f);
		    return res;
		}
		
	}

}
