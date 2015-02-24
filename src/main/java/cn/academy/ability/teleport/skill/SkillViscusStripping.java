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
import cn.academy.misc.entity.EntityMarker;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;

public class SkillViscusStripping extends SkillBase {
	
	public SkillViscusStripping() {
		setName("tp_visc");
		setLogo("tp/viscus_stripping.png");
		setMaxLevel(10);
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
	
	private static double getDist(int slv, int lv) {
		return 8 + slv * .8 + lv * 3;
	}
	
	public static class ViscusState extends PatternHold.State {
		
		final AbilityData data;
		EntityMarker mark;

		public ViscusState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
		}

		@Override
		public void onStart() {
			if(isRemote()) {
				player.worldObj.spawnEntityInWorld(mark = new EntityMarker(player));
				mark.r = 1;
				mark.g = mark.b = 0.2f;
				mark.a = 0.8f;
			}
		}

		@Override
		public void onFinish() {
			if(mark != null)
				mark.setDead();
			
			int slv = data.getSkillLevel(CatTeleport.skillViscusStripping), lv = data.getLevelID() + 1;
			float csm = 400 + lv * 45 + slv * 50;
			if(!data.decreaseCP(csm))
				return;
			
			double dist = getDist(slv, lv);
			MovingObjectPosition mop = performTrace();
			if(mop != null && mop.typeOfHit == MovingObjectType.ENTITY) {
				if(mop.entityHit instanceof EntityLivingBase) {
					float dmg = (float) GenericUtils.randIntv(slv + lv * 1.2, slv * 1.2 + lv * 1.8);
					mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), dmg);
					if(isRemote()) {
						EntityBloodSplash.genSplashEffect(mop.entityHit);
					} else {
						//反♂胃
						if(rand.nextDouble() < 0.25)
							player.addPotionEffect(new PotionEffect(Potion.confusion.id, 100));
					}
					player.playSound("academy:tp.tp", 0.5f, 1.0f);
				}
			}
		}
		
		@Override
		public boolean onTick(int ticks) {
			if(isRemote()) {
				int slv = data.getSkillLevel(CatTeleport.skillViscusStripping), lv = data.getLevelID() + 1;
				double dist = getDist(slv, lv);
				MovingObjectPosition mop = performTrace();
				if(mop == null) {
					Motion3D mo = new Motion3D(player, true);
					mo.move(dist);
					mark.forceSetPos(mo.posX, mo.posY, mo.posZ);
				} else if(mop.typeOfHit == MovingObjectType.ENTITY) {
					mark.target = mop.entityHit;
				} else {
					mark.forceSetPos(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
				}
			}
			return false;
		}
		
		private MovingObjectPosition performTrace() {
			int slv = data.getSkillLevel(CatTeleport.skillViscusStripping), lv = data.getLevelID() + 1;
			double dist = getDist(slv, lv);
			Motion3D mo = new Motion3D(player, true);
			MovingObjectPosition mop = GenericUtils.rayTraceBlocksAndEntities(GenericUtils.selectorLiving,
				player.worldObj, 
				mo.getPosVec(player.worldObj), 
				mo.move(dist).getPosVec(player.worldObj), 
				player);
			return mop;
		}

		@Override
		public void onHold() {}
		
	}
	
}
