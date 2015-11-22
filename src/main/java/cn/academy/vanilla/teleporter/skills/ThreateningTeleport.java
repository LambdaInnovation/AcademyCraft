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

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.BlockSelectors;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

/**
 * @author WeAthFolD
 */
public class ThreateningTeleport extends Skill {

	static final Color COLOR_NORMAL = new Color().fromHexColor(0xbabababa),
			COLOR_THREATENING = new Color().fromHexColor(0xbab2232a);

	public static final ThreateningTeleport instance = new ThreateningTeleport();

	public ThreateningTeleport() {
		super("threatening_teleport", 1);
	}

	public static float getRange(AbilityData data) {
		return instance.callFloatWithExp("range", data);
	}

	public static float getExpIncr(boolean attacked) {
		return (attacked ? 1 : 0.2f) * instance.getFloat("expincr");
	}

	public static float getDamage(AbilityData data, ItemStack stack) {
		float dmg = instance.callFloatWithExp("damage", data);
		if (stack.getItem() == ModuleVanilla.needle) {
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

	public static class ThreateningAction extends SkillSyncAction {

		boolean attacked;

		public ThreateningAction() {
			super(-1);
		}

		@Override
		public void onStart() {
			super.onStart();

			if (!isRemote && player.getCurrentEquippedItem() == null) {
				ActionManager.abortAction(this);
			}

			if (isRemote) {
				startEffects();
			}
		}

		@Override
		public void onTick() {
			if (isRemote) {
				updateEffects();
			}

			if (!isRemote && player.getCurrentEquippedItem() == null) {
				ActionManager.abortAction(this);
			}
		}

		@Override
		public void onEnd() {
			ItemStack curStack = player.getCurrentEquippedItem();
			if (curStack != null && cpData.perform(instance.getOverload(aData), instance.getConsumption(aData))) {
				attacked = true;
				TraceResult result = calcDropPos();

				if (!isRemote) {
					double dropProb = 1.0;
					boolean attacked = false;

					if (result.target != null) {
						attacked = true;
						TPAttackHelper.attack(player, instance, result.target, getDamage(aData, curStack));
						instance.triggerAchievement(player);
						dropProb = 0.3;
					}

					if (!player.capabilities.isCreativeMode) {
						if (--curStack.stackSize <= 0) {
							player.setCurrentItemOrArmor(0, null);
						}
					}

					if (RandUtils.ranged(0, 1) < dropProb) {
						ItemStack drop = curStack.copy();
						drop.stackSize = 1;
						player.worldObj.spawnEntityInWorld(
								new EntityItem(player.worldObj, result.x, result.y, result.z, drop));
					}
					aData.addSkillExp(instance, getExpIncr(attacked));
				}

				setCooldown(instance, instance.getCooldown(aData));
			}

			if (isRemote) {
				endEffects();
			}
		}

		@Override
		public void onAbort() {
			if (isRemote) {
				endEffects();
			}
		}

		TraceResult calcDropPos() {
			double range = getRange(aData);

			MovingObjectPosition pos = Raytrace.traceLiving(player, range, EntitySelectors.living,
					BlockSelectors.filEverything);
			if (pos == null)
				pos = Raytrace.traceLiving(player, range, EntitySelectors.nothing);

			TraceResult ret = new TraceResult();
			if (pos == null) {
				Motion3D mo = new Motion3D(player, true).move(range);
				ret.setPos(mo.px, mo.py, mo.pz);
			} else if (pos.typeOfHit == MovingObjectType.BLOCK) {
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
			marker.width = marker.height = 0.5f;
		}

		@SideOnly(Side.CLIENT)
		void updateEffects() {
			TraceResult res = calcDropPos();
			if (res.target != null)
				res.y -= res.target.height;
			marker.setPosition(res.x, res.y, res.z);
			marker.target = res.target;
			marker.color = marker.target != null ? COLOR_THREATENING : COLOR_NORMAL;
		}

		@SideOnly(Side.CLIENT)
		void endEffects() {
			marker.setDead();
			if (attacked) {
				ACSounds.playClient(player, "tp.tp", 0.5f);

				TraceResult dropPos = calcDropPos();
				double dx = dropPos.x + .5 - player.posX, dy = dropPos.y + .5 - (player.posY - 0.5),
						dz = dropPos.z + .5 - player.posZ;
				double dist = MathUtils.length(dx, dy, dz);
				Motion3D mo = new Motion3D(player.posX, player.posY - 0.5, player.posZ, dx, dy, dz);
				mo.normalize();

				double move = 1;
				for (double x = move; x <= dist; x += (move = RandUtils.ranged(1, 2))) {
					mo.move(move);
					player.worldObj.spawnEntityInWorld(TPParticleFactory.instance.next(player.worldObj, mo.getPosVec(),
							VecUtils.vec(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .05),
									RandUtils.ranged(-.02, .02))));
				}
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
