/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.SRElecBase;
import cn.academy.ability.electro.entity.EntityElecArcFX;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.proxy.ACClientProps;
import cn.weaponmod.api.damage.RayDamageApplier;
import cn.weaponmod.api.damage.RayDamageFactory;
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
		Entity arc;
		
		public StateArc(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			//N/A
			System.out.println("OnStart");
		}

		@Override
		public void onFinish() {
			World world = player.worldObj;
			if(tick <= SINGLE_DT) { //Single click
				if(isRemote()) {
					world.spawnEntityInWorld(new EntityElecArcFX(world, player).setLifetime(3));
				} else  {
					float dmg = 5F;
					//TODO calculate damage
					System.out.println("Sent");
					new RayDamageApplier(player, 5).attempt();
				}
				
			} else { //Hold
				arc.setDead();
			}
			System.out.println("OnEnd");
		}

		@Override
		public void onHold() {
			++tick;
			World world = player.worldObj;
			
			System.out.println("OnHold");
			if(tick == SINGLE_DT + 1) { //Start hold
				System.out.println("Start hold");
				if(isRemote()) {
					arc = (EntityElecArcFX) new EntityElecArcFX(world, player).setFollowPlayer(true);
					world.spawnEntityInWorld(arc);
				} else {
					float perdmg = 3F;
					arc = new RayDamageFactory.Periodic(player, perdmg, 3);
					world.spawnEntityInWorld(arc);
				}
			}
		}
		
	}
}
