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
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class PenetrateTeleport extends Skill {

	public static final PenetrateTeleport instance = new PenetrateTeleport();

	private PenetrateTeleport() {
		super("penetrate_teleport", 2);
	}

	private static boolean hasPlace(World world, double x, double y, double z) {
		int ix = (int) x, iy = (int) y, iz = (int) z;
		Block b1 = world.getBlock(ix, iy, iz), b2 = world.getBlock(ix, iy + 1, iz);

		return !b1.canCollideCheck(world.getBlockMetadata(ix, iy, iz), false)
				&& !b2.canCollideCheck(world.getBlockMetadata(ix, iy + 1, iz), false);
	}

	static Dest getDest(AbilityData data) {
		EntityPlayer player = data.getEntity();
		CPData cpData = CPData.get(player);
		World world = player.worldObj;
		double dist = getMaxDistance(data);
		double cplim = cpData.getCP() / instance.getConsumption(data);
		dist = Math.min(dist, cplim);

		final double STEP = 0.8;
		int stage = 0;
		int counter = 0;
		Motion3D mo = new Motion3D(player, true);
		for (double totalStep = 0.0; totalStep <= dist; totalStep += STEP, mo.move(STEP)) {
			boolean b = hasPlace(world, mo.px, mo.py, mo.pz);
			if (stage == 0) {
				if (!b)
					stage = 1;
			} else if (stage == 1) {
				if (b)
					stage = 2;
			} else {
				if (!b || (++counter > 4))
					break;
			}
		}

		return new Dest(mo.getPosVec(), stage != 1);
	}

	static float getMaxDistance(AbilityData data) {
		return instance.callFloatWithExp("max_distance", data);
	}

	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		PenetrateAction action = new PenetrateAction();
		return new SkillInstance() {
			AbilityData aData = AbilityData.get(player);

			@Override
			public void onTick() {
				if (action.mark == null) // Action not yet started
					return;
				if (action.mark.available) {
					float dist = (float) getPlayer().getDistance(action.mark.posX, action.mark.posY, action.mark.posZ);
					estimatedCP = dist * instance.getConsumption(aData);
				} else {
					estimatedCP = 0;
				}
			}
		}.addChild(action);
	}

	public static class PenetrateAction extends SkillSyncAction {

		// Final calculated dest
		Dest dest;

		public PenetrateAction() {
			super(-1);
		}

		@Override
		public void onStart() {
			super.onStart();

			if (isRemote)
				startEffects();
		}

		@Override
		public void onTick() {
			if (isRemote)
				updateEffects();
		}

		@Override
		public void writeNBTFinal(NBTTagCompound tag) {
			dest = getDest(aData);
			tag.setBoolean("a", dest.available);
			tag.setFloat("x", (float) dest.pos.xCoord);
			tag.setFloat("y", (float) dest.pos.yCoord);
			tag.setFloat("z", (float) dest.pos.zCoord);
		}

		@Override
		public void readNBTFinal(NBTTagCompound tag) {
			dest = new Dest(VecUtils.vec(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z")), tag.getBoolean("a"));
		}

		@Override
		public void onEnd() {
			if (!dest.available) {
				onAbort();
				return;
			}

			if (isRemote)
				endEffects();

			double x = dest.pos.xCoord, y = dest.pos.yCoord, z = dest.pos.zCoord;
			double distance = player.getDistance(x, y, z);
			if (isRemote) {
				player.setPosition(x, y, z);
				ACSounds.playClient(player, "tp.tp", .5f);
				setCooldown(instance, instance.getCooldown(aData));
			} else {
				cpData.performWithForce(instance.getOverload(aData),
						(float) (distance * instance.getConsumption(aData)));
				aData.addSkillExp(instance, instance.getFunc("expincr").callFloat(distance));
				ModuleAchievements.trigger(player, "teleporter.ignore_barrier");
				TPAttackHelper.incrTPCount(player);

				player.setPositionAndUpdate(x, y, z);
				player.fallDistance = 0;
			}
		}

		@Override
		public void onAbort() {
			if (isRemote)
				endEffects();
		}

		// CLIENT
		EntityTPMarking mark;

		@SideOnly(Side.CLIENT)
		private void startEffects() {
			if (isLocal()) {
				player.worldObj.spawnEntityInWorld(mark = new EntityTPMarking(player));
			}
		}

		@SideOnly(Side.CLIENT)
		private void updateEffects() {
			if (isLocal()) {
				Dest dest = getDest(aData);
				mark.available = dest.available;
				mark.setPosition(dest.pos.xCoord, dest.pos.yCoord, dest.pos.zCoord);
			}
		}

		@SideOnly(Side.CLIENT)
		private void endEffects() {
			if (mark != null) {
				mark.setDead();
			}
		}

	}

	private static class Dest {
		final Vec3 pos;
		final boolean available;

		public Dest(Vec3 _pos, boolean _available) {
			pos = _pos;
			available = _available;
		}
	}

}
