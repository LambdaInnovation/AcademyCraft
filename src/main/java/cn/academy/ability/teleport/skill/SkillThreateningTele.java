/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.teleport.skill;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.misc.entity.EntityMarker;
import cn.academy.misc.item.ItemNeedle;
import cn.academy.misc.util.ACUtils;
import cn.academy.misc.util.DamageHelper;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;

@RegistrationClass
public class SkillThreateningTele extends SkillBase {

	private static SkillThreateningTele instance;

	public SkillThreateningTele() {
		instance = this;
		setLogo("tp/threatening.png");
		setName("tp_threatening");
		setMaxLevel(10);
	}

	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new ThreateningState(player);
			}

		}.setCooldown(500));
	}

	private static class ThreateningState extends SkillState {
		
		EntityMarker marker;
		final double dist;
		final int slv, lv;

		public ThreateningState(EntityPlayer player) {
			super(player);
			AbilityData data = AbilityDataMain.getData(player);
			slv = data.getSkillLevel(data.getSkillID(instance));
			lv = data.getLevelID() + 1;
			dist = 8 + slv * .5 + lv * .8;
		}

		@Override
		public void onStart() {
			if(isRemote()) {
				player.worldObj.spawnEntityInWorld(marker = new EntityMarker(player));
				marker.r = 29 / 255.0f;
				marker.g = 138 / 255.0f;
				marker.b = 215 / 255.0f;
				marker.a = 0.7f;
			}
		}
		
		@Override
		public boolean onTick(int tick) {
			if(player.getCurrentEquippedItem() == null) {
				if(isRemote()) {
					marker.setDead();
					ACUtils.playAbortSound();
				}
				return true;
			}
			
			if(isRemote()) {
				Object[] dest = getTraceDest();
				if(dest[3] != null) {
					marker.target = (Entity) dest[3];
				} else {
					marker.forceSetPos((Double) dest[0], (Double) dest[1], (Double) dest[2]);
				}
			}
			return false;
		}
		
		public boolean onFinish(boolean fin) {
			if(isRemote()) {
				marker.setDead();
			}
			
			ItemStack stack = player.getCurrentEquippedItem();
			AbilityData data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(data.getSkillID(instance)), lv = data.getLevelID() + 1;
			if(stack == null || (slv <= 3 && stack.getItem() instanceof ItemBlock)) {
				return false;
			}
			
			float ccp = 250F - slv * 20F + lv * 150F;
			if(!data.decreaseCP(ccp, instance)) {
				if(isRemote()) {
					ACUtils.playAbortSound();
				}
				
				return false;
			}
			player.worldObj.playSoundAtEntity(player, "academy:tp.tp", 1f, 1f);
			
			Object[] dest = getTraceDest();
			double x = (Double) dest[0],
					y = (Double) dest[1],
					z = (Double) dest[2];
			Entity targ = (Entity) dest[3];
			if(targ != null) {
				//hit entity
				float damage = 2F + slv * .4F + lv * .6F;
				if (stack.getItem() instanceof ItemNeedle) {
					damage *= 1.5F;
				}
				DamageHelper.applyEntityDamage(targ, DamageSource.causePlayerDamage(player), damage);
				if(!isRemote() && rand.nextDouble() < 0.2) {
					dropItem(stack, x, y, z);
				}
			} else {
				//drop item at the far end
				if(!isRemote()) {
					dropItem(stack, x, y, z);
				}
			}
			
			if(!player.capabilities.isCreativeMode) {
				--stack.stackSize;
				if(stack.stackSize <= 0) {
					player.setCurrentItemOrArmor(0, null);
				}
			}
			return true;
		}
		
		private Object[] getTraceDest() {
			MovingObjectPosition mop = GenericUtils.tracePlayerWithEntities(player, dist, null);
			double x, y, z;
			Entity e = null;
			if(mop == null || mop.typeOfHit == MovingObjectType.BLOCK) {
				Motion3D mo = new Motion3D(player, true);
				mo.move(dist);
				x = mo.posX;
				y = mo.posY;
				z = mo.posZ;
			} else {
				x = mop.entityHit.posX;
				y = mop.entityHit.posY;
				z = mop.entityHit.posZ;
				e = mop.entityHit;
			}
			
			return new Object[] { x, y, z, e };
		}
		
		private double[] simpleTrace() { //Sacrifice a bit of performance for simplicity.
			Object[] comp = getTraceDest();
			return new double[] { (Double) comp[0], (Double) comp[1], (Double) comp[2] };
		}
		
		private void dropItem(ItemStack stack, double x, double y, double z) {
			ItemStack newItemStack = stack.copy();
			newItemStack.stackSize = 1;
			EntityItem entityitem = new EntityItem(player.worldObj, x, y, z, newItemStack);
			entityitem.delayBeforeCanPickup = 10;
			player.worldObj.spawnEntityInWorld(entityitem);
		}

	}
	
}
