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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.entity.EntityBlock;
import cn.academy.core.util.DamageHelper;
import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.handlers.Rigidbody;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.mc.WorldUtils;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class ShiftTeleport extends Skill {
	
	static final Color CRL_BLOCK_MARKER = new Color().setColor4i(139, 139, 139, 180),
			CRL_ENTITY_MARKER = new Color().setColor4i(235, 81, 81, 180);
	
	static ShiftTeleport instance;

	public ShiftTeleport() {
		super("shift_tp", 4);
		instance = this;
	}
	
	public static float getDamage(AbilityData data) {
		return instance.callFloatWithExp("damage", data);
	}
	
	public static float getRange(AbilityData data) {
		return instance.callFloatWithExp("range", data);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new ShiftTPAction());
	}
	
	public static class ShiftTPAction extends SyncAction {
		
		AbilityData aData;
		CPData cpData;

		public ShiftTPAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
			
			if(isRemote) startEffects();
		}
		
		@Override
		public void onTick() {
			if(isRemote) updateEffects();
		}
		
		@Override
		public void onEnd() {
			if(isRemote) endEffects();
			
			if(!isRemote) {
				Vec3 dest = getTraceDest();
				ItemStack stack = player.getCurrentEquippedItem();
				Block block;
				
				if(stack != null && (block = Block.getBlockFromItem(stack.getItem())) != null
						&& cpData.perform(instance.getOverload(aData), instance.getConsumption(aData))) {
					EntityBlock entity = new STEntityBlock(player.worldObj);
					entity.fromItemStack(stack);
					entity.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);
					
					List<Entity> list = getTargetsInLine();
					for(Entity target : list) {
						DamageHelper.attack(target, DamageSource.causePlayerDamage(player), getDamage(aData));
					}
					
					player.worldObj.spawnEntityInWorld(entity);
					player.worldObj.playSoundAtEntity(player, "academy:tp.tp", 0.5f, 1f);
					
					if(!player.capabilities.isCreativeMode) {
						if(stack.stackSize-- == 0) {
							player.setCurrentItemOrArmor(0, null);
						}
					}
				}
			}
		}
		
		private Vec3 getTraceDest() {
			double range = getRange(aData);
			MovingObjectPosition result = Raytrace.traceLiving(player, range, EntitySelectors.nothing);
			if(result != null)
				return result.hitVec;
			return new Motion3D(player, true).move(range).getPosVec();
		}
		
		private List<Entity> getTargetsInLine() {
			double range = getRange(aData);
			Vec3 v0 = VecUtils.vec(player.posX, player.posY, player.posZ), v1 = getTraceDest();
			
			AxisAlignedBB area = WorldUtils.ofPoints(VecUtils.vec(player.posX, player.posY, player.posZ), getTraceDest());
			IEntitySelector selector = new IEntitySelector() {

				@Override
				public boolean isEntityApplicable(Entity entity) {
					double hw = entity.width / 2;
					return VecUtils.checkLineBox(VecUtils.vec(entity.posX - hw, entity.posY, entity.posZ - hw),
						VecUtils.vec(entity.posX + hw, entity.posY + entity.height, entity.posZ + hw),
						v0, v1) != null;
				}
				
			};
			return WorldUtils.getEntities(player.worldObj, area, 
				EntitySelectors.combine(EntitySelectors.living, EntitySelectors.excludeOf(player), selector));
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		EntityMarker blockMarker;
		
		@SideOnly(Side.CLIENT)
		List<EntityMarker> targetMarkers = new ArrayList();
		
		@SideOnly(Side.CLIENT)
		int effTicker;
		
		@SideOnly(Side.CLIENT)
		void startEffects() {
			if(isLocal()) {
				blockMarker = new EntityMarker(player.worldObj);
				blockMarker.ignoreDepth = true;
				blockMarker.width = blockMarker.height = 1.2f;
				blockMarker.color = CRL_BLOCK_MARKER;
				blockMarker.setPosition(player.posX, player.posY, player.posZ);
				
				player.worldObj.spawnEntityInWorld(blockMarker);
			}
		}
		
		@SideOnly(Side.CLIENT)
		void updateEffects() {
			if(isLocal()) {
				if(++effTicker == 3) {
					effTicker = 0;
					for(EntityMarker em : targetMarkers) {
						em.setDead();
					}
					targetMarkers.clear();
					List<Entity> targetsInLine = getTargetsInLine();
					for(Entity e : targetsInLine) {
						EntityMarker em = new EntityMarker(e);
						em.color = CRL_ENTITY_MARKER;
						em.ignoreDepth = true;
						player.worldObj.spawnEntityInWorld(em);
						targetMarkers.add(em);
					}
				}
				
				Vec3 dest = getTraceDest();
				blockMarker.setPosition(dest.xCoord, dest.yCoord - 0.2, dest.zCoord);
			}
		}
		
		@SideOnly(Side.CLIENT)
		void endEffects() {
			if(isLocal()) {
				for(EntityMarker em : targetMarkers)
					em.setDead();
				blockMarker.setDead();
			}
		}
		
	}
	
	@RegEntity
	public static class STEntityBlock extends EntityBlock {
		
		public STEntityBlock(World world) {
			super(world);
		}
		
		{
			Rigidbody rb = getMotionHandler(Rigidbody.class);
			rb.gravity = 0.04;
		}
		
	}

}
