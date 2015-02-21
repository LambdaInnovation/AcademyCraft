package cn.academy.ability.teleport.skill;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.ability.teleport.CatTeleport;
import cn.academy.ability.teleport.entity.fx.EntityBloodSplash;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;

public class SkillViscusStripping extends SkillBase {
	
	public SkillViscusStripping() {
		setName("tp_visc");
		setLogo("tp/viscus_stripping.png");
		setMaxSkillLevel(10);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {
			@Override
			public State createSkill(EntityPlayer player) {
				return new ViscusState(player);
			}
		});
	}
	
	public static class ViscusState extends PatternHold.State {
		
		final AbilityData data;

		public ViscusState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
		}

		@Override
		public void onStart() {
			
		}

		@Override
		public void onFinish() {
			int slv = data.getSkillLevel(CatTeleport.skillViscusStripping), lv = data.getLevelID() + 1;
			float csm = 400 + lv * 45 + slv * 50;
			if(!data.decreaseCP(csm))
				return;
			
			double dist = 8 + slv * .8 + lv * 3;
			Motion3D mo = new Motion3D(player, true);
			MovingObjectPosition mop = GenericUtils.rayTraceBlocksAndEntities(GenericUtils.selectorLiving,
				player.worldObj, 
				mo.getPosVec(player.worldObj), 
				mo.move(dist).getPosVec(player.worldObj), 
				player);
			if(mop != null && mop.typeOfHit == MovingObjectType.ENTITY) {
				if(mop.entityHit instanceof EntityLivingBase) {
					float dmg = (float) GenericUtils.randIntv(slv + lv * 1.2, slv * 1.2 + lv * 1.8);
					mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), dmg);
					if(isRemote()) {
						EntityBloodSplash.genSplashEffect(mop.entityHit);
					} else {
						//反♂胃
						player.addPotionEffect(new PotionEffect(Potion.confusion.id, 100));
					}
					player.playSound("academy:tp.tp", 0.5f, 1.0f);
				}
			}
		}

		@Override
		public void onHold() {}
		
	}
	
}
