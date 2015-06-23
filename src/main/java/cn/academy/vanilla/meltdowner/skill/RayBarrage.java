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
package cn.academy.vanilla.meltdowner.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.CPData;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.vanilla.meltdowner.entity.EntityBarrageRayPre;
import cn.academy.vanilla.meltdowner.entity.EntityMdRayBarrage;
import cn.academy.vanilla.meltdowner.entity.EntitySilbarn;
import cn.liutils.entityx.event.CollideEvent;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.raytrace.Raytrace;

/**
 * @author WeAthFolD
 */
public class RayBarrage extends Skill {
	
	static final double RAY_DIST = 20;
	
	static RayBarrage instance;

	public RayBarrage() {
		super("ray_barrage", 3);
		
		instance = this;
	}
	
	private static float getPlainDamage(AbilityData data) {
		return 10.0f;
	}
	
	private static float getScatteredDamage(AbilityData data) {
		return 3.0f;
	}
	
	private static float getConsumption(AbilityData data) {
		return 10.0f;
	}
	
	private static float getOverload(AbilityData data) {
		return 10.0f;
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant().addExecution(new BarrageAction());
	}
	
	public static class BarrageAction extends SyncActionInstant {
		
		boolean hit;
		EntitySilbarn silbarn;

		@Override
		public boolean validate() {
			CPData cData = CPData.get(player);
			AbilityData aData = AbilityData.get(player);
			
			MovingObjectPosition pos = Raytrace.traceLiving(player, RAY_DIST);
			if(pos != null && pos.entityHit instanceof EntitySilbarn) {
				hit = true;
				silbarn = (EntitySilbarn) pos.entityHit;
			}
			
			return cData.perform(getOverload(aData), getConsumption(aData));
		}
		
		public void readNBTFinal(NBTTagCompound tag) {
			hit = tag.getBoolean("h");
			if(hit) {
				silbarn = (EntitySilbarn) player.worldObj.getEntityByID(tag.getInteger("e"));
			}
		}
		
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setBoolean("h", hit);
			if(hit) {
				tag.setInteger("e", silbarn.getEntityId());
			}
		}

		@Override
		public void execute() {
			double tx, ty, tz;
			if(hit) {
				if(silbarn == null)
					return;
				tx = silbarn.posX;
				ty = silbarn.posY;
				tz = silbarn.posZ;
				
				// Post the collide event to make it break. Might a bit hacking
				silbarn.postEvent(new CollideEvent(new MovingObjectPosition(silbarn)));
				
				if(isRemote) {
					player.worldObj.spawnEntityInWorld(
						new EntityMdRayBarrage(player.worldObj, tx, ty, tz, 
							player.rotationYaw, player.rotationPitch));
					
				} else {
					// Do the damage
				}
				
			} else {
				Motion3D mo = new Motion3D(player, true).move(RAY_DIST);
				tx = mo.px;
				ty = mo.py;
				tz = mo.pz;
			}
			
			if(isRemote) {
				EntityBarrageRayPre raySmall = new EntityBarrageRayPre(player.worldObj);
				raySmall.setFromTo(player.posX, player.posY, player.posZ, tx, ty, tz);
				player.worldObj.spawnEntityInWorld(raySmall);
			}
		}
		
	}

}
