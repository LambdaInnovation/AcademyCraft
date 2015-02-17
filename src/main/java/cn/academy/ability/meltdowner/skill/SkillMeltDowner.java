/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import cn.academy.ability.meltdowner.entity.EntityMeltDowner;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;

/**
 * @author WeathFolD
 *
 */
public class SkillMeltDowner extends SkillBase {

	public SkillMeltDowner() {
		this.setLogo("meltdowner/meltdown.png");
		this.setName("md_meltdown");
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(120) {
			@Override
			public State createSkill(EntityPlayer player) {
				return new MDState(player);
			}
		});
	}
	
	public static class MDState extends PatternHold.State {
		
		//TODO: Current used built-in abort logic. Change when API update.
		final AbilityData data;
		boolean spawn = true;
		
		EntityMeltDowner mdRay;

		public MDState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
		}

		@Override
		public void onStart() {
			//if(!isRemote())
		}

		@Override
		public void onFinish() {
			if(!spawn) {
				return;
			}
			if(!isRemote()) {
				player.worldObj.spawnEntityInWorld(new EntityMeltDowner(player, 5.0f));
			}
		}
		
		@Override
		public boolean onTick(int ticks) {
			if(!data.decreaseCP(100)) {
				spawn = false;
				return true;
			}
			if(ticks == 100) {
				player.attackEntityFrom(DamageSource.causePlayerDamage(player), 
					Math.min(player.getHealth() - 0.1f, 19.0f));
				spawn = false;
				return true;
			}
			return false;
		}

		@Override
		public void onHold() {}
		
	}

}
