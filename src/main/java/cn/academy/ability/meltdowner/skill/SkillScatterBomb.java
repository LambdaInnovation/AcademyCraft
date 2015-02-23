/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
import cn.academy.ability.meltdowner.entity.EntityWeakRay;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.GenericUtils;

/**
 * @author WeathFolD
 *
 */
public class SkillScatterBomb extends SkillBase {

	/**
	 * 
	 */
	public SkillScatterBomb() {
		this.setLogo("meltdowner/scatter.png");
		this.setName("md_scatter");
	}
	
	private static float getDamage(int slv, int lv) {
		return (float) GenericUtils.randIntv(slv * 0.3 + lv * 0.5, slv * 0.5 + lv);
	}
	
	private static float getCPConsume(int slv, int lv) { //Per tick.
		return 13 + slv * 1.2f + lv;
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(201) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new SBState(player);
			}
			
		});
	}
	
	public static class SBState extends State {
		
		final AbilityData data;
		final float ccp;
		List<EntityMdBall> balls = new ArrayList();

		public SBState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
			ccp = getCPConsume(
				data.getSkillLevel(CatMeltDowner.scatterBomb),
				data.getLevelID() + 1);
		}

		@Override
		public void onStart() {
			// NOPE
		}

		@Override
		public void onFinish() {
			final boolean b = this.getTickTime() < 200;
			final int slv = data.getSkillLevel(CatMeltDowner.scatterBomb), lv = data.getLevelID() + 1;
			for(EntityMdBall ball : balls) {
				if(b) {
					player.worldObj.spawnEntityInWorld(new EntityWeakRay(player, ball, getDamage(slv, lv), 10));
				}
				ball.setDead();
			}
			if(!b) {
				//player.attackEntityFrom(DamageSource.causePlayerDamage(player), 
				//		Math.min(player.getHealth() - 1f, 10f));
			}
		}
		
		@Override
		public boolean onTick(int ticks) {
			if(ticks >= 240)
				return true;
			if(!isRemote() && ticks <= 110 && (ticks - 20) % 15 == 0) {
				EntityMdBall ball = new EntityMdBall(player);
				balls.add(ball);
				player.worldObj.spawnEntityInWorld(ball);
			}
			return ticks <= 110 && !data.decreaseCP(ccp, CatMeltDowner.scatterBomb);
		}

		@Override
		public void onHold() {}
		
	}
}
