/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.proxy.ACClientProps;

/**
 * 对手上的物品充能
 * TODO 施工中
 * @author WeathFolD
 */
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
	
	private static class StateHold extends State {

		public StateHold(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() { }

		@Override
		public void onFinish() { }

		@Override
		public void onHold() {
			//this.player.xxxxxx
		}
		
	}

}
