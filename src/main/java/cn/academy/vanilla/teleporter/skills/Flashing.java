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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.SpecialSkill;
import cn.academy.ability.api.SubSkill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.academy.vanilla.teleporter.util.GravityCancellor;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.util.deprecated.LIFMLGameEventDispatcher;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class Flashing extends SpecialSkill {

	public static final Flashing instance = new Flashing();
	static List<Movement> movements = new ArrayList();

	private Flashing() {
		super("flashing", 5);
		this.addSubSkill(new Movement(Keyboard.KEY_A, "a", VecUtils.vec(0, 0, -1)));
		this.addSubSkill(new Movement(Keyboard.KEY_D, "d", VecUtils.vec(0, 0, 1)));
		this.addSubSkill(new Movement(Keyboard.KEY_W, "w", VecUtils.vec(1, 0, 0)));
		this.addSubSkill(new Movement(Keyboard.KEY_S, "s", VecUtils.vec(-1, 0, 0)));
	}

	public static float getRange(AbilityData aData) {
		return instance.callFloatWithExp("range", aData);
	}

	private static void addMovement(Movement m) {
		movements.add(m);
		m.id = movements.size() - 1;
	}

	@Override
	protected SpecialSkillAction getSpecialAction(EntityPlayer player) {
		return new FlashingAction();
	}

	public static class FlashingAction extends SpecialSkillAction {

		@SideOnly(Side.CLIENT)
		GravityCancellor cancellor;

		public FlashingAction() {
			super(instance, -1);
		}

		@Override
		public void onSkillTick() {
			if (isRemote)
				updateClient();
		}

		@SideOnly(Side.CLIENT)
		private void updateClient() {
			if (cancellor != null && cancellor.isDead())
				cancellor = null;
		}

	}

	static class Movement extends SubSkill {

		int id;
		final Vec3 direction;

		public Movement(int key, String _name, Vec3 _dir) {
			super(_name);
			setRemapped(key);
			direction = _dir;
			addMovement(this);
		}

		@Override
		public SkillInstance createSkillInstance(EntityPlayer player) {
			return new SkillInstance() {
				@Override
				public void onStart() {
					addChild(new MovementAction(Movement.this));
				}
			};
		}

		@Override
		public boolean shouldOverrideKey() {
			return false;
		}

	}

	public static class MovementAction extends SkillSyncAction {

		Movement movement;

		public MovementAction(Movement _m) {
			super(-1);
			movement = _m;
		}

		public MovementAction() {
			super(-1);
		}

		@Override
		public void writeNBTStart(NBTTagCompound tag) {
			tag.setByte("i", (byte) movement.id);
		}

		@Override
		public void readNBTStart(NBTTagCompound tag) {
			movement = movements.get(tag.getByte("i"));
		}

		@Override
		public void onStart() {
			super.onStart();

			if (isRemote) {
				startEffects();
			}
		}

		@Override
		public void onTick() {
			if (isRemote) {
				updateEffects();
			} else {
				if (!cpData.canPerform(instance.getConsumption(aData)))
					ActionManager.abortAction(this);
			}
		}

		@Override
		public void onEnd() {
			if (!isRemote) {
				Vec3 dest = getDest();
				player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
				player.fallDistance = 0.0f;

				cpData.perform(instance.getOverload(aData), instance.getConsumption(aData));
				aData.addSkillExp(instance, instance.getFloat("expincr"));
				instance.triggerAchievement(player);
				TPAttackHelper.incrTPCount(player);
			} else {
				setCooldown(movement, 5);
			}
		}

		@Override
		public void onFinalize() {
			if (isRemote) {
				endEffects();
			}
		}

		private Vec3 getDest() {
			double dist = getRange(aData);

			Vec3 dir = VecUtils.copy(movement.direction);
			dir.rotateAroundZ(player.rotationPitch * MathUtils.PI_F / 180);
			dir.rotateAroundY((-90 - player.rotationYaw) * MathUtils.PI_F / 180);

			Motion3D mo = new Motion3D(player.posX, player.posY, player.posZ, dir.xCoord, dir.yCoord, dir.zCoord);

			MovingObjectPosition mop = Raytrace.perform(world, mo.getPosVec(), mo.move(dist).getPosVec(),
					EntitySelectors.and(EntitySelectors.living, EntitySelectors.excludeOf(player)));

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
				x = mo.px;
				y = mo.py;
				z = mo.pz;
			}

			return VecUtils.vec(x, y, z);
		}

		// CLIENT
		@SideOnly(Side.CLIENT)
		EntityTPMarking marking;

		@SideOnly(Side.CLIENT)
		private void startEffects() {
			if (isLocal()) {
				marking = new EntityTPMarking(player);
				world.spawnEntityInWorld(marking);
			}
		}

		@SideOnly(Side.CLIENT)
		private void updateEffects() {
			if (isLocal()) {
				Vec3 dest = getDest();
				marking.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);
			}
		}

		@SideOnly(Side.CLIENT)
		private void endEffects() {
			if (isLocal()) {
				marking.setDead();
			}
			if (this.getState() == State.ENDED) {
				ACSounds.playClient(player, "tp.tp_flashing", 1.0f);
				FlashingAction env = ActionManager.findAction(player, FlashingAction.class);
				if (env != null) {
					if (env.cancellor != null)
						env.cancellor.setDead();
					LIFMLGameEventDispatcher.INSTANCE
							.registerClientTick(env.cancellor = new GravityCancellor(player, 40));
				}
			}
		}

	}

}
