/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.entity.EntityArcBullet;
import cn.academy.ability.electro.entity.EntityElecArcFX;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 一般电弧攻击
 * @author WeathFolD
 *
 */
public class SkillElecArc extends SkillBase {
	
	static final int MAX_HOLD_TIME = 200;

	public SkillElecArc() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(MAX_HOLD_TIME) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new StateArc(player);
			}
			
		});
	}
	
	public String getInternalName() {
		return "em_arc";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_ARC;
	}

	public static class StateArc extends State {

		int tick;
		static final int SINGLE_DT = 2; //判定为单点的最长允许按键tick
		static final int BULLET_RATE = 4;
		EntityElecArcFX arc;
		
		public StateArc(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			//N/A
			System.out.println("Start " + isRemote());
		}

		@Override
		public void onFinish() {
			World world = player.worldObj;
			if(tick <= SINGLE_DT) {
				if(isRemote()) {
					world.spawnEntityInWorld(new EntityElecArcFX(world, player));
				} else  {
					float dmg = 5F;
					//TODO calculate damage
					world.spawnEntityInWorld(new EntityArcBullet(world, player, dmg));
				}
			} else {
				if(isRemote()) {
					arc.setDead();
				}
			}
			System.out.println("End " + isRemote());
		}

		@Override
		public void onHold() {
			++tick;
			World world = player.worldObj;
			float perdmg = 3F;
			if(tick >= SINGLE_DT + 1) {
				if(isRemote() && tick == SINGLE_DT + 1) {
					arc = (EntityElecArcFX) new EntityElecArcFX(world, player).setFollowPlayer(true);
				}
				if(!isRemote() && (tick - SINGLE_DT - 1) % BULLET_RATE == 0) {
					world.spawnEntityInWorld(new EntityArcBullet(world, player, perdmg));
				}
			}
		}
		
	}
}
