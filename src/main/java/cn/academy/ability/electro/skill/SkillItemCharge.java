/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.skill.SRSmallCharge;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.client.render.SkillRenderManager;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 对手上的物品充能
 * TODO 施工中 现在是实验场
 * @author WeathFolD
 */
@RegistrationClass
public class SkillItemCharge extends SkillBase {

	public SkillItemCharge() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(100) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new StateHold(player);
			}

		});
	}
	
	public String getInternalName() {
		return "em_itemcharge";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_CHARGE;
	}
	
	public static class StateHold extends State {

		public StateHold(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() { 
			World world = player.worldObj;
			if(world.isRemote) {
//				EntityArcS arc = EntityArcS.get(world);
//				arc.setPosition(player.posX, player.posY, player.posZ);
//				arc.addDaemonHandler(new LifeTime(arc, 30));
				world.spawnEntityInWorld(new ChargeEffectS(player, 40, 5));
				SkillRenderManager.addEffect(new SRSmallCharge(5, 0.8), 1000);
			}
		}

		@Override
		public void onFinish() { }

		@Override
		public void onHold() {
			//this.player.xxxxxx
		}
		
	}

}
