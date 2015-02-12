/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.skill.SRSmallCharge;
import cn.academy.ability.electro.entity.AttackingArcBase;
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
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 强电弧（电弧束）攻击
 * @author WeathFolD
 */
@RegistrationClass
public class SkillStrongArc extends SkillBase {
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge = new SRSmallCharge(5, 0.8);
	
	public SkillStrongArc() {
		setLogo("electro/arc_strong.png");
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

	public static class StateArc extends SkillState {
		
		public StateArc(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if(!player.worldObj.isRemote) {
				if(consumeCP()){
					player.worldObj.spawnEntityInWorld(new WeakArc(player));
					player.playSound("academy:elec.strong", 0.5F, 1.0F);
				}
			} else {
				if(consumeCP()) {
					SkillRenderManager.addEffect(charge, 500);
					player.worldObj.spawnEntityInWorld(new ChargeEffectS(player, 40, 5));
					player.playSound("academy:elec.strong", 0.5F, 1.0F);
				}
			}
		}
		
		private boolean consumeCP() {
			AbilityData data = AbilityDataMain.getData(player);
			int id = 1, lv = data.getSkillLevel(id), clv = data.getLevelID() + 1;
			float need = 340 + lv * 25 + clv * 30;
			return data.decreaseCP(need);
		}

		@Override
		public void onFinish() {}
		
	}
	
	@RegEntity
	public static class WeakArc extends AttackingArcBase {

		public WeakArc(EntityPlayer creator) {
			super(creator);
		}
		
		public WeakArc(World world) {
			super(world);
		}

		@Override
		protected SkillBase getSkill() {
			return CatElectro.weakArc;
		}

		@Override
		protected float getDamage(int slv, int lv) {
			return 16 + slv * 0.8F + lv * 1.2F;
		}

		@Override
		protected double getAOERange(int slv, int lv) {
			return 7 + lv * 0.6 + slv * 1;
		}
		
		@Override
		protected double getIgniteProb(int slv, int lv) {
			return 0.3 + slv * 0.08;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public ResourceLocation[] getTexs() {
			return ACClientProps.ANIM_ELEC_ARC_STRONG;
		}

		@Override
		protected int getLifetime() {
			return 11;
		}
		
	}
	


}
