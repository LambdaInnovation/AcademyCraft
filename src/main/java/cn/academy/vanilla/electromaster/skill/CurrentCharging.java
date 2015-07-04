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
package cn.academy.vanilla.electromaster.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyItemHelper;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Current charging/电流回充
 * @author WeAthFolD
 */
public class CurrentCharging extends Skill {

	static CurrentCharging instance;
	
	public CurrentCharging() {
		super("charging", 2);
		instance = this;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance() {
			@Override
			public void onStart() {
				ItemStack stack = getPlayer().getCurrentEquippedItem();
				
				SyncAction action;
				if(stack == null) {
					action = new ActionChargeBlock();
				} else {
					action = new ActionChargeItem();
				}
				
				ActionManager.startAction(action);
				this.addChild(action);
			}
		};
	}
	
	public static float getChargingSpeed(AbilityData data) {
		return call("speed", data);
	}
	
	public static float getConsumption(AbilityData data) {
		return call("consumption", data);
	}
	
	public static float getOverload(AbilityData data) {
		return call("overload", data);
	}
	
	public static float getExpIncr(AbilityData data, boolean effective) {
		return call("exp_incr_" + (effective ? "effective" : "ineffective"), data);
	}
	
	private static float call(String name, AbilityData data) {
		return instance.getFunc(name).callFloat(data.getSkillExp(instance));
	}
	
	public static class ActionChargeBlock extends SyncAction {
		
		AbilityData aData;
		CPData cpData;

		public ActionChargeBlock() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
			
			cpData.perform(getOverload(aData), 0);
		}
		
		@Override
		public void onTick() {
			// Perform raytrace 
			MovingObjectPosition pos = Raytrace.traceLiving(player, 15.0d);
			
			boolean good;
			if(pos != null && pos.typeOfHit == MovingObjectType.BLOCK) {
				TileEntity tile = player.worldObj.getTileEntity(pos.blockX, pos.blockY, pos.blockZ);
				if(EnergyBlockHelper.isSupported(tile)) {
					good = true;
					
					// Very well, charge the block
					if(!isRemote) {
						float charge = getChargingSpeed(aData);
						EnergyBlockHelper.charge(tile, charge, true);
					}
				} else {
					good = false;
				}
			} else {
				good = false;
			}
			
			cpData.perform(0, getConsumption(aData));
			aData.addSkillExp(instance, getExpIncr(aData, good));
			
			if(isRemote)
				updateEffects(pos, good);
		}
		
		@Override
		public void onEnd() {
			if(isRemote)
				endEffects();
		}
		
		@Override
		public void onAbort() {
			if(isRemote)
				endEffects();
		}
		
		//CLIENT
		@SideOnly(Side.CLIENT)
		EntityArc arc;
		
		@SideOnly(Side.CLIENT)
		EntitySurroundArc surround;
		
		@SideOnly(Side.CLIENT)
		private void startEffects() {
			player.worldObj.spawnEntityInWorld(arc = new EntityArc(player));
			player.worldObj.spawnEntityInWorld(
				surround = new EntitySurroundArc(player.worldObj, player.posX, player.posY, player.posZ, 1, 1));
		}
		
		@SideOnly(Side.CLIENT)
		private void updateEffects(MovingObjectPosition res, boolean isGood) {
			double x = res.hitVec.xCoord, y = res.hitVec.yCoord, z = res.hitVec.zCoord;
			if(res.typeOfHit == MovingObjectType.ENTITY) {
				y += res.entityHit.getEyeHeight();
			}
			
			if(isGood) {
				surround.updatePos(res.blockX + 0.5, res.blockY, res.blockZ + 0.5);
				surround.draw = true;
			} else {
				surround.draw = false;
			}
		}
		
		@SideOnly(Side.CLIENT)
		private void endEffects() {
			surround.setDead();
			arc.setDead();
		}
		
	}
	
	public static class ActionChargeItem extends SyncAction {
		
		AbilityData aData;
		CPData cpData;

		protected ActionChargeItem() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
		}
		
		@Override
		public void onTick() {
			ItemStack stack = player.getCurrentEquippedItem();
			if(stack != null) {
				float cp = getConsumption(aData);
				float amt = getChargingSpeed(aData);
				if(EnergyItemHelper.isSupported(stack))
					EnergyItemHelper.charge(stack, amt, false);
				
				cpData.perform(0, cp);
			} else {
				ActionManager.endAction(this);
			}
		}
	}

}
