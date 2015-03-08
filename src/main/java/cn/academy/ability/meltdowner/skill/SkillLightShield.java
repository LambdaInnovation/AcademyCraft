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

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMdShield;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Light shield skill
 * @author WeathFolD
 */
public class SkillLightShield extends SkillBase {
	
	private static Map<EntityPlayer, EntityMdShield> table = new HashMap(); //Used in server side

	public SkillLightShield() {
		this.setLogo("meltdowner/shield.png");
		this.setName("md_shield");
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	private static float getCCP(int slv, int lv) {
		return 35 - slv * 0.4f - lv * 1.2f;
	}
	
	/**
	 * Return the percent that a damage will be holded by the shield.
	 */
	private static double getHoldAmt(int slv, int lv) {
		return Math.min(1, .4 + slv * .03 + lv * .05);
	}
	
	/**
	 * Return the max dmg that this shield can hold.
	 */
	private static double getHoldMax(int slv, int lv) {
		return 8 + slv * 2 + lv * 4;
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new LSState(player);
			}
			
		}.setCooldown(50));
	}
	
	@SubscribeEvent
	public void onLivingAttack(LivingAttackEvent e) {
		EntityMdShield shield;
		if((shield = table.get(e.entityLiving)) != null) {
			if(shield.canHold()) {
				AbilityData data = AbilityDataMain.getData((EntityPlayer) e.entityLiving);
				int slv = data.getSkillLevel(CatMeltDowner.shield), lv = data.getLevelID() + 1;
				double r = getHoldAmt(slv, lv);
				float reduce = (float) Math.min(getHoldMax(slv, lv), r * e.ammount);
				shield.setHolded(); //Called first to prevent infinite recursive(?)
				e.setCanceled(true);
				e.entityLiving.attackEntityFrom(e.source, e.ammount - reduce);
			}
		}
	}
	
	public static class LSState extends State {
		
		final AbilityData data;
		final float ccp, dmgl, dmgr;
		
		//Spawn only in server
		EntityMdShield shield;

		public LSState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(CatMeltDowner.shield), lv = data.getLevelID() + 1;
			ccp = getCCP(slv, lv);
			dmgl = slv * 0.3f + lv * 0.5f;
			dmgr = slv * 0.5f + lv;
		}

		@Override
		public void onStart() {
			if(!isRemote()) {
				player.worldObj.spawnEntityInWorld(shield = new EntityMdShield(player, dmgl, dmgr));
				table.put(player, shield);
			}
		}

		@Override
		public boolean onFinish(boolean fin) {
			if(!isRemote()) {
				shield.setDead();
				table.remove(player);
			}
			return true;
		}
		
		@Override
		public boolean onTick(int ticks) {
			return !data.decreaseCP(ccp, CatMeltDowner.shield);
		}

		@Override
		public void onHold() {}
		
	}

}
