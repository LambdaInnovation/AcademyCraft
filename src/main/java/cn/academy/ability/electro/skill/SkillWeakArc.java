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
import cn.annoreg.mc.RegSubmoduleInit;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 一般电弧攻击
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
public class SkillWeakArc extends SkillBase {
	
	static final int MAX_HOLD_TIME = 200;
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge;
	
	@SideOnly(Side.CLIENT)
	public static void init() {
		charge = new SRSmallCharge(5, 0.8);
	}
	
	public SkillWeakArc() {
		setLogo("electro/arc.png");
		setName("em_arc");
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

	public static class StateArc extends SkillState {
		
		public StateArc(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			if(!player.worldObj.isRemote) {
				if(consumeCP()){
					player.worldObj.spawnEntityInWorld(
						new WeakArc(player));
					player.playSound("academy:elec.weak", 0.5F, 1.0F);
				}
			} else {
				if(consumeCP()) {
					SkillRenderManager.addEffect(charge, 600);
					player.worldObj.spawnEntityInWorld(new ChargeEffectS(player, 15, 5));
					player.playSound("academy:elec.weak", 0.5F, 1.0F);
				}
			}
		}
		
		private boolean consumeCP() {
			AbilityData data = AbilityDataMain.getData(player);
			int id = data.getSkillID(CatElectro.weakArc), lv = data.getSkillLevel(id), clv = data.getLevelID() + 1;
			float need = 250 - lv * (21 - lv) + 10 * clv * (15 - clv);
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
			return lv + slv * 0.5f;
		}

		@Override
		protected double getAOERange(int slv, int lv) {
			return 3 + lv * 0.3 + slv * 0.5;
		}
		
		@Override
		protected double getIgniteProb(int slv, int lv) {
			return 0.2 + slv * 0.05;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public ResourceLocation[] getTexs() {
			return ACClientProps.ANIM_ELEC_ARC;
		}

		@Override
		protected int getLifetime() {
			return 7;
		}
		
	}
}
