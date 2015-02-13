/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
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
	}
	
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new BombState(player);
			}
			
		});
	}
	
	private static float getConsume(int slv, int lv) {
		return 250 + slv *  40 + lv * 20;
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
					player.worldObj.spawnEntityInWorld(new EntityMdBall(player));
				}
			}
		}

		@Override
		public void onFinish() {}

		@Override
		public void onHold() {}
		
	}

}
