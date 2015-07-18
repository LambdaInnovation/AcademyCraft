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
package cn.academy.vanilla.teleporter.skills;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.util.DamageHelper;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class ThreateningTeleport extends Skill {
	
	static final Color
		COLOR_NORMAL = new Color().fromHexColor(0xbabababa),
		COLOR_THREATENING = new Color().fromHexColor(0xbab2232a);
	
	static ThreateningTeleport instance;

	public ThreateningTeleport() {
		super("threatening_teleport", 1);
		instance = this;
	}
	
	public static float getRange(AbilityData data) {
		return instance.callFloatWithExp("range", data);
	}
	
	public static float getDamage(AbilityData data, ItemStack stack) {
		float dmg = instance.callFloatWithExp("damage", data);
		if(stack.getItem() == ModuleVanilla.needle) {
			dmg *= 1.5f;
		}
		return dmg;
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance() {
			
			@Override
			public void onStart() {
				this.estimatedCP = instance.getConsumption(AbilityData.get(getPlayer()));
			}
			
		}.addChild(new ThreateningAction());
	}
	
	public static class ThreateningAction extends SyncAction {
		
		AbilityData aData;
		CPData cpData;
		
		boolean attacked;

		public ThreateningAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
			
			if(!isRemote && player.getCurrentEquippedItem() == null)
				ActionManager.abortAction(this);
			
			if(isRemote) startEffects();
		}
		
		@Override
		public void onTick() {
			if(isRemote) updateEffects();
			
			if(!isRemote && player.getCurrentEquippedItem() == null)
				ActionManager.abortAction(this);
		}
		
		@Override
		public void onEnd() {
			ItemStack curStack = player.getCurrentEquippedItem();
			if(curStack != null &&
				cpData.perform(instance.getOverload(aData), instance.getConsumption(aData))) {
				attacked = true;
				TraceResult result = calcDropPos();
				
				double dropProb = 1.0;
				if(result.target != null) {
					DamageHelper.attack(result.target, 
						DamageSource.causePlayerDamage(player), getDamage(aData, curStack));
					dropProb = 0.3;
				}
				
				if(!isRemote) {
					if(!player.capabilities.isCreativeMode) {
						if(--curStack.stackSize == 0) {
							player.setCurrentItemOrArmor(0, null);
						}
					}
					
					if(RandUtils.ranged(0, 1) < dropProb) {
						ItemStack drop = curStack.copy();
						drop.stackSize = 1;
						player.worldObj.spawnEntityInWorld(
							new EntityItem(player.worldObj, result.x, result.y, result.z, drop));
					}
				}
			}
			
			if(isRemote) endEffects();
		}
		
		@Override
		public void onAbort() {
			if(isRemote) endEffects();
		}
		
		TraceResult calcDropPos() {
			double range = getRange(aData);
			MovingObjectPosition pos = Raytrace.traceLiving(player, range);
			TraceResult ret = new TraceResult();
			if(pos == null) {
				Motion3D mo = new Motion3D(player, true).move(range);
				ret.setPos(mo.px, mo.py, mo.pz);
			} else if(pos.typeOfHit == MovingObjectType.BLOCK) {
				ret.setPos(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord);
			} else {
				Entity ent = pos.entityHit;
				ret.setPos(ent.posX, ent.posY + ent.height, ent.posZ);
				ret.target = ent;
			}
			return ret;
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		EntityMarker marker;
		
		@SideOnly(Side.CLIENT)
		void startEffects() {
			player.worldObj.spawnEntityInWorld(marker = new EntityMarker(player.worldObj));
			marker.setPosition(player.posX, player.posY, player.posZ);
		}
		
		@SideOnly(Side.CLIENT)
		void updateEffects() {
			TraceResult res = calcDropPos();
			if(res.target != null)
				res.y -= res.target.height;
			marker.setPosition(res.x, res.y, res.z);
			marker.target = res.target;
			marker.color = marker.target != null ? COLOR_THREATENING : COLOR_NORMAL;
		}
		
		@SideOnly(Side.CLIENT)
		void endEffects() {
			marker.setDead();
			if(attacked) {
				ACSounds.playAtEntity(player, "tp.tp", 0.5f);
			}
		}
		
	}
	
	private static class TraceResult {
		double x, y, z;
		Entity target;
		
		public void setPos(double _x, double _y, double _z) {
			x = _x;
			y = _y;
			z = _z;
		}
	}

}
