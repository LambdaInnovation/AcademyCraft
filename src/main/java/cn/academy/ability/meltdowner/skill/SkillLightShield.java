/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMdShield;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.GenericUtils;

/**
 * Light shield skill
 * @author WeathFolD
 */
public class SkillLightShield extends SkillBase {

	public SkillLightShield() {
		this.setLogo("meltdowner/shield.png");
		this.setName("md_shield");
		setMaxLevel(15);
	}
	
	private static float getCCP(int slv, int lv) {
		return 35 - slv * 0.4f - lv * 1.2f;
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new LSState(player);
			}
			
		});
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
			}
		}

		@Override
		public void onFinish() {
			if(!isRemote()) {
				shield.setDead();
			}
		}
		
		@Override
		public boolean onTick(int ticks) {
			return !data.decreaseCP(ccp, CatMeltDowner.shield);
		}

		@Override
		public void onHold() {}
		
	}

}
