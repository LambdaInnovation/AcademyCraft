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

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author WeAthFolD
 */
@Registrant
public class ShiftTeleport extends Skill {

	static final Color CRL_BLOCK_MARKER = new Color().setColor4i(139, 139, 139, 180),
			CRL_ENTITY_MARKER = new Color().setColor4i(235, 81, 81, 180);

	public static final ShiftTeleport instance = new ShiftTeleport();

	private ShiftTeleport() {
		super("shift_tp", 4);
	}

	static float getExpIncr(int attackEntities) {
		return instance.getFunc("expincr").callFloat(attackEntities);
	}

	public static float getDamage(AbilityData data) {
		return instance.callFloatWithExp("damage", data);
	}

	public static float getRange(AbilityData data) {
		return instance.callFloatWithExp("range", data);
	}

	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new ShiftTPAction())
				.setEstmCP(instance.getConsumption(AbilityData.get(player)));
	}

	public static class ShiftTPAction extends SkillSyncAction {

		public ShiftTPAction() {
			super(-1);
		}

		@Override
		public void onStart() {
			super.onStart();

			ItemStack stack = player.getCurrentEquippedItem();
			Block block;
			if (!(stack != null && stack.getItem() instanceof ItemBlock
					&& (Block.getBlockFromItem(stack.getItem())) != null))
				ActionManager.abortAction(this);

			if (isRemote)
				startEffects();
		}

		@Override
		public void onTick() {
			if (isRemote)
				updateEffects();
		}

		boolean attacked;

		@Override
		public void onEnd() {
			if (isRemote)
				return;

			Block block;
			ItemStack stack = player.getCurrentEquippedItem();
			attacked = stack != null && stack.getItem() instanceof ItemBlock
					&& (block = Block.getBlockFromItem(stack.getItem())) != null;
			if (!attacked)
				return;

			ItemBlock item = (ItemBlock) stack.getItem();
			MovingObjectPosition position = getTracePosition();

			if (item.field_150939_a.canPlaceBlockAt(player.worldObj, position.blockX, position.blockY, position.blockZ)
					&& cpData.perform(instance.getOverload(aData), instance.getConsumption(aData))) {

				item.placeBlockAt(stack, player, player.worldObj, position.blockX, position.blockY, position.blockZ,
						position.sideHit, (float) position.hitVec.xCoord, (float) position.hitVec.yCoord,
						(float) position.hitVec.zCoord, stack.getItemDamage());

				if (!player.capabilities.isCreativeMode) {
					if (--stack.stackSize <= 0)
						player.setCurrentItemOrArmor(0, null);
				}

				List<Entity> list = getTargetsInLine();
				for (Entity target : list) {
					TPAttackHelper.attack(player, instance, target, getDamage(aData));
				}

				player.worldObj.playSoundAtEntity(player, "academy:tp.tp_shift", 0.5f, 1f);
				aData.addSkillExp(instance, getExpIncr(list.size()));

				if (!player.capabilities.isCreativeMode) {
					if (stack.stackSize-- == 0) {
						player.setCurrentItemOrArmor(0, null);
					}
				}

				setCooldown(instance, instance.getCooldown(aData));
			}
		}

		@Override
		public void onFinalize() {
			if (isRemote)
				endEffects();
		}

		// TODO: Some boilerplate... Clean this up in case you aren't busy
		private int[] getTraceDest() {
			double range = getRange(aData);
			MovingObjectPosition result = Raytrace.traceLiving(player, range, EntitySelectors.nothing);
			if (result != null) {
				ForgeDirection dir = ForgeDirection.values()[result.sideHit];
				return new int[] { result.blockX + dir.offsetX, result.blockY + dir.offsetY,
						result.blockZ + dir.offsetZ };
			}
			Motion3D mo = new Motion3D(player, true).move(range);
			return new int[] { (int) mo.px, (int) mo.py, (int) mo.pz };
		}

		private MovingObjectPosition getTracePosition() {
			double range = getRange(aData);
			MovingObjectPosition result = Raytrace.traceLiving(player, range, EntitySelectors.nothing);
			if (result != null) {
				ForgeDirection dir = ForgeDirection.values()[result.sideHit];
				result.blockX += dir.offsetX;
				result.blockY += dir.offsetY;
				result.blockZ += dir.offsetZ;
				return result;
			}
			Motion3D mo = new Motion3D(player, true).move(range);
			return new MovingObjectPosition((int) mo.px, (int) mo.py, (int) mo.pz, 0,
					VecUtils.vec(mo.px, mo.py, mo.pz));
		}

		private List<Entity> getTargetsInLine() {
			double range = getRange(aData);
			int[] dest = getTraceDest();
			Vec3 v0 = VecUtils.vec(player.posX, player.posY, player.posZ),
					v1 = VecUtils.vec(dest[0] + .5, dest[1] + .5, dest[2] + .5);

			AxisAlignedBB area = WorldUtils.minimumBounds(v0, v1);
			IEntitySelector selector = new IEntitySelector() {

				@Override
				public boolean isEntityApplicable(Entity entity) {
					double hw = entity.width / 2;
					return VecUtils.checkLineBox(VecUtils.vec(entity.posX - hw, entity.posY, entity.posZ - hw),
							VecUtils.vec(entity.posX + hw, entity.posY + entity.height, entity.posZ + hw), v0,
							v1) != null;
				}

			};
			return WorldUtils.getEntities(player.worldObj, area,
					EntitySelectors.and(EntitySelectors.living, EntitySelectors.excludeOf(player), selector));
		}

		// CLIENT
		@SideOnly(Side.CLIENT)
		EntityMarker blockMarker;

		@SideOnly(Side.CLIENT)
		List<EntityMarker> targetMarkers;

		@SideOnly(Side.CLIENT)
		int effTicker;

		@SideOnly(Side.CLIENT)
		void startEffects() {
			targetMarkers = new ArrayList();
			if (isLocal()) {
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
			if (isLocal()) {
				if (++effTicker == 3) {
					effTicker = 0;
					for (EntityMarker em : targetMarkers) {
						em.setDead();
					}
					targetMarkers.clear();
					List<Entity> targetsInLine = getTargetsInLine();
					for (Entity e : targetsInLine) {
						EntityMarker em = new EntityMarker(e);
						em.color = CRL_ENTITY_MARKER;
						em.ignoreDepth = true;
						player.worldObj.spawnEntityInWorld(em);
						targetMarkers.add(em);
					}
				}

				int[] dest = getTraceDest();
				blockMarker.setPosition(dest[0] + 0.5, dest[1], dest[2] + 0.5);
			}
		}

		@SideOnly(Side.CLIENT)
		void endEffects() {
			if (isLocal()) {
				for (EntityMarker em : targetMarkers)
					em.setDead();
				blockMarker.setDead();
			}

			if (attacked) {
				int[] dest = getTraceDest();
				double dx = dest[0] + .5 - player.posX, dy = dest[1] + .5 - (player.posY - 0.5),
						dz = dest[2] + .5 - player.posZ;
				double dist = MathUtils.length(dx, dy, dz);
				Motion3D mo = new Motion3D(player.posX, player.posY - 0.5, player.posZ, dx, dy, dz);
				mo.normalize();

				double move = 1;
				for (double x = move; x <= dist; x += (move = RandUtils.ranged(0.6, 1))) {
					mo.move(move);
					player.worldObj.spawnEntityInWorld(TPParticleFactory.instance.next(player.worldObj, mo.getPosVec(),
							VecUtils.vec(RandUtils.ranged(-.05, .05), RandUtils.ranged(-.02, .05),
									RandUtils.ranged(-.05, .05))));
				}
			}
		}

	}
}
