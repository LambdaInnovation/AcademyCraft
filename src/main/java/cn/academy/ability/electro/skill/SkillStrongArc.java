/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.client.render.skill.SRSmallCharge;
import cn.academy.ability.electro.entity.EntityStrongArc;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.render.SkillRenderManager;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 强电弧（电弧束）攻击
 * @author WeathFolD
 */
public class SkillStrongArc extends SkillBase {
	
	public static SkillStrongArc instance;
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge = new SRSmallCharge(5, 0.8);
	
	public SkillStrongArc() {
		instance = this;
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new StateArc(player);
			}
			
		});
	}
	
	public String getInternalName() {
		return "em_arc_strong";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_ARC_STRONG;
	}

	public static class StateArc extends SkillState {
		
		public StateArc(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if(!player.worldObj.isRemote) {
				if(consumeCP()){
					player.worldObj.spawnEntityInWorld(
						new EntityStrongArc(player, instance));
				}
			} else {
				if(consumeCP()) {
					player.worldObj.spawnEntityInWorld(
						new EntityStrongArc.OffSync(player, instance));
					SkillRenderManager.addEffect(charge, 500);
					player.worldObj.spawnEntityInWorld(new ChargeEffectS(player, 40, 5));
				}
			}
		}
		
		private boolean consumeCP() {
			AbilityData data = AbilityDataMain.getData(player);
			int id = data.getSkillID(instance), lv = data.getSkillLevel(id), clv = data.getLevelID() + 1;
			float need = 340+ lv *25 + clv *30;
			return data.decreaseCP(need);
		}

		@Override
		public void onFinish() {}
		
	}

}
