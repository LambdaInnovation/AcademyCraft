/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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
 * 强电弧（电弧束）攻击
 * @author WeathFolD
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
public class SkillStrongArc extends SkillBase {
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge;
	
	@SideOnly(Side.CLIENT)
	public static void init() {
		charge = new SRSmallCharge(5, 0.8);
	}
	
	public SkillStrongArc() {
		setLogo("electro/arc_strong.png");
		setName("em_arc_strong");
		setMaxLevel(10);
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
					player.worldObj.spawnEntityInWorld(new StrongArc(player));
					player.playSound("academy:elec.strong", 0.5F, 1.0F);
				}
			} else {
				if(consumeCP()) {
					SkillRenderManager.addEffect(charge, 500);
					player.worldObj.spawnEntityInWorld(new ChargeEffectS.Strong(player, 40, 5));
					player.playSound("academy:elec.strong", 0.5F, 1.0F);
				}
			}
		}
		
		private boolean consumeCP() {
			AbilityData data = AbilityDataMain.getData(player);
			int id = 1, slv = data.getSkillLevel(id), lv = data.getLevelID() + 1;
			float need = 1000 + slv * 60 + lv * 80;
			return data.decreaseCP(need, CatElectro.strongArc);
		}

		@Override
		public boolean onFinish() {
			return true;
		}
		
	}
	
	@RegEntity
	public static class StrongArc extends AttackingArcBase {

		public StrongArc(EntityPlayer creator) {
			super(creator);
		}
		
		public StrongArc(World world) {
			super(world);
		}

		@Override
		protected SkillBase getSkill() {
			return CatElectro.strongArc;
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
