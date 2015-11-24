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

import java.util.List;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.vanilla.meltdowner.entity.EntityBarrageRayPre;
import cn.academy.vanilla.meltdowner.entity.EntityMdRayBarrage;
import cn.academy.vanilla.meltdowner.entity.EntitySilbarn;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.entityx.event.CollideEvent;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * @author WeAthFolD
 */
@Registrant
public class RayBarrage extends Skill {
	
	static final double DISPLAY_RAY_DIST = 20;
	static final double RAY_DIST = 20;
	
	public static final RayBarrage instance = new RayBarrage();

	private RayBarrage() {
		super("ray_barrage", 4);

		FMLCommonHandler.instance().bus().register(this);
	}
	
	//TODO Sounds
	
	private static float getPlainDamage(AbilityData data) {
		return instance.callFloatWithExp("plain_damage", data);
	}
	
	private static float getScatteredDamage(AbilityData data) {
		return instance.callFloatWithExp("scatter_damage", data);
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
			
			MovingObjectPosition pos = Raytrace.traceLiving(player, DISPLAY_RAY_DIST);
			if(pos != null && pos.entityHit instanceof EntitySilbarn && !((EntitySilbarn)pos.entityHit).isHit()) {
				hit = true;
				silbarn = (EntitySilbarn) pos.entityHit;
			}
			
			return cData.perform(instance.getOverload(aData), instance.getConsumption(aData));
		}
		
		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			hit = tag.getBoolean("h");
			if(hit) {
				silbarn = (EntitySilbarn) player.worldObj.getEntityByID(tag.getInteger("e"));
			}
		}
		
		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setBoolean("h", hit);
			if(hit) {
				tag.setInteger("e", silbarn.getEntityId());
			}
		}

		@Override
		public void execute() {
			AbilityData aData = AbilityData.get(player);
			
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
					spawnBarrage();
				} else {
					// Do the damage
					float range = 55;
					
					float yaw = player.rotationYaw;
					float pitch = player.rotationPitch;
					
					float minYaw = yaw - range / 2, maxYaw = yaw + range / 2;
					float minPitch = pitch - range, maxPitch = pitch + range;
					
					IEntitySelector selector = EntitySelectors.excludeOf(silbarn, player);
					
					Motion3D mo = new Motion3D(player.posX, player.posY, player.posZ);
					
					Vec3 v0 = mo.getPosVec(),
						v1 = mo.clone().fromRotation(minYaw, minPitch).move(RAY_DIST).getPosVec(),
						v2 = mo.clone().fromRotation(minYaw, maxPitch).move(RAY_DIST).getPosVec(),
						v3 = mo.clone().fromRotation(maxYaw, maxPitch).move(RAY_DIST).getPosVec(),
						v4 = mo.clone().fromRotation(maxYaw, minPitch).move(RAY_DIST).getPosVec();
					
					AxisAlignedBB aabb = WorldUtils.minimumBounds(v0, v1, v2, v3, v4);
					
					List<Entity> list = WorldUtils.getEntities(player.worldObj, aabb, selector);
					for(Entity e : list) {
						// Double check whether the entity is within range.
						double dx = e.posX - player.posX;
						double dy = (e.posY + e.getEyeHeight()) - (player.posY + player.getEyeHeight());
						double dz = e.posZ - player.posZ;
						
						mo.setMotion(dx, dy, dz);
						float eyaw = mo.getRotationYaw(), epitch = mo.getRotationPitch();
						
						if(MathUtils.angleYawinRange(minYaw, maxYaw, eyaw) && (minPitch <= epitch && epitch <= maxPitch)) {
							MDDamageHelper.attack(e, player, getScatteredDamage(aData));
						}
					}
				}
				
			} else {
				Motion3D mo = new Motion3D(player, true).move(DISPLAY_RAY_DIST);
				tx = mo.px;
				ty = mo.py;
				tz = mo.pz;
				
				if(!isRemote) {
					MovingObjectPosition result = Raytrace.traceLiving(player, RAY_DIST);
					if(result != null && result.entityHit != null) {
						MDDamageHelper.attack(result.entityHit, player, getPlainDamage(aData));
					}
				}
				
			}
			
			if(isRemote) {
				spawnPreRay(player.posX, player.posY, player.posZ, tx, ty, tz);
			}
			
			setCooldown(instance, instance.getCooldown(aData));
			aData.addSkillExp(instance, instance.getFloat("expincr"));
		}
		
		@SideOnly(Side.CLIENT)
		private void spawnPreRay(double x0, double y0, double z0, double x1, double y1, double z1) {
			EntityBarrageRayPre raySmall = new EntityBarrageRayPre(player.worldObj, hit);
			raySmall.setFromTo(x0, y0, z0, x1, y1, z1);
			player.worldObj.spawnEntityInWorld(raySmall);
		}
		
		@SideOnly(Side.CLIENT)
		private void spawnBarrage() {
			player.worldObj.spawnEntityInWorld(
					new EntityMdRayBarrage(player.worldObj, silbarn.posX, silbarn.posY, silbarn.posZ, 
						player.rotationYaw, player.rotationPitch));
		}
		
	}

}
