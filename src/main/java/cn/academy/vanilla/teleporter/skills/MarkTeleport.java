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
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

/**
 * @author WeAthFolD
 */
public class MarkTeleport extends Skill {

	public static final MarkTeleport instance = new MarkTeleport();

	private static double MINIMUM_VALID_DISTANCE = 3.0;

	private MarkTeleport() {
		super("mark_teleport", 2);
	}

	public static double getMaxDist(AbilityData data, CPData cpData, int ticks) {
		double max = instance.callFloatWithExp("range", data);
		double cplim = cpData.getCP() / getCPB(data);

		return Math.min((ticks + 1) * 2, Math.min(max, cplim));
	}

	/**
	 * @return Consumption per block
	 */
	public static float getCPB(AbilityData data) {
		return instance.getConsumption(data);
	}

	private static Vec3 getDest(EntityPlayer player, int ticks) {
		double dist = getMaxDist(AbilityData.get(player), CPData.get(player), ticks);
		MovingObjectPosition mop = Raytrace.traceLiving(player, dist);

		double x, y, z;

		if (mop != null) {
			x = mop.hitVec.xCoord;
			y = mop.hitVec.yCoord;
			z = mop.hitVec.zCoord;

			if (mop.typeOfHit == MovingObjectType.BLOCK) {
				switch (mop.sideHit) {
				case 0:
					y -= 1.0;
					break;
				case 1:
					y += 1.8;
					break;
				case 2:
					z -= .6;
					y = mop.blockY + 1.7;
					break;
				case 3:
					z += .6;
					y = mop.blockY + 1.7;
					break;
				case 4:
					x -= .6;
					y = mop.blockY + 1.7;
					break;
				case 5:
					x += .6;
					y = mop.blockY + 1.7;
					break;
				}
				// check head
				if (mop.sideHit > 1) {
					int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
					if (!player.worldObj.isAirBlock(hx, hy, hz)) {
						y -= 1.25;
					}
				}
			} else {
				y += mop.entityHit.getEyeHeight();
			}
		} else {
			Motion3D mo = new Motion3D(player, true).move(dist);
			x = mo.px;
			y = mo.py;
			z = mo.pz;
		}

		return VecUtils.vec(x, y, z);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public SkillInstance createSkillInstance(EntityPlayer player) {
		MTAction action = new MTAction();
		AbilityData data = AbilityData.get(player);

		return new SkillInstance() {
			@Override
			public void onTick() {
				if (action.mark == null) {
					return;
				}

				float distance = (float) MathUtils.distance(action.mark.posX, action.mark.posY, action.mark.posZ,
						player.posX, player.posY, player.posZ);
				this.estimatedCP = distance * getCPB(data);
			}
		}.addChild(action);
	}

	public static class MTAction extends SkillSyncAction {

		int ticks;

		public MTAction() {
			super(-1);
		}

		@Override
		public void onStart() {
			// if(true) return;
			super.onStart();

			if (isRemote) {
				startEffects();
			}
		}

		@Override
		public void onTick() {
			ticks++;

			if (isRemote) {
				updateEffects(getDest(player, ticks));
			}
		}

		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			tag.setShort("t", (short) ticks);
		}

		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			ticks = tag.getShort("t");
		}

		@Override
		public void onEnd() {
			Vec3 dest = getDest(player, ticks);
			float distance = (float) dest.distanceTo(VecUtils.vec(player.posX, player.posY, player.posZ));
			if (distance < MINIMUM_VALID_DISTANCE) {
				// TODO: Play abort sound
				;
			} else {

				cpData.performWithForce(instance.getOverload(aData), distance * getCPB(aData));

				if (!isRemote) {
					((EntityPlayerMP) player).setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
					aData.addSkillExp(instance, instance.getFunc("expincr").callFloat(distance));
					player.fallDistance = 0;
				} else {
					ACSounds.playClient(player, "tp.tp", .5f);
				}

				setCooldown(instance, instance.getCooldown(aData));
				TPAttackHelper.incrTPCount(player);
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

		// CLIENT
		@SideOnly(Side.CLIENT)
		EntityTPMarking mark;

		@SideOnly(Side.CLIENT)
		private void startEffects() {
			if (isLocal()) {
				player.worldObj.spawnEntityInWorld(mark = new EntityTPMarking(player));
			}
		}

		@SideOnly(Side.CLIENT)
		private void updateEffects(Vec3 dest) {
			if (isLocal()) {
				mark.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);
			}
		}

		@SideOnly(Side.CLIENT)
		private void endEffects() {
			if (isLocal())
				mark.setDead();
		}

	}

}
