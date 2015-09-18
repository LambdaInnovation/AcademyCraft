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

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.vanilla.meltdowner.entity.EntityMdBall;
import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.RangedTarget;
import cn.liutils.entityx.EntityCallback;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@Registrant
public class ElectronBomb extends Skill {
	
	static ElectronBomb instance;

	static final int LIFE = 20, LIFE_IMPROVED = 5;
	static final double DISTANCE = 15;
	
	public ElectronBomb() {
		super("electron_bomb", 1);
		instance = this;
	}
	
	static float getDamage(AbilityData data) {
		return instance.callFloatWithExp("damage", data);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant().addChild(new EBAction());
	}
	
	public static class EBAction extends SyncActionInstant {

		@Override
		public boolean validate() {
			return cpData.perform(instance.getOverload(aData), 
					instance.getConsumption(aData));
		}

		@Override
		public void execute() {
			if(!isRemote) {
				EntityMdBall ball = new EntityMdBall(player, aData.getSkillExp(instance) >= 0.8f ? LIFE_IMPROVED : LIFE, 
				new EntityCallback<EntityMdBall>() {

					@Override
					public void execute(EntityMdBall ball) {
						MovingObjectPosition trace = Raytrace.perform(world, VecUtils.vec(ball.posX, ball.posY, ball.posZ), getDest(player), 
							EntitySelectors.combine(EntitySelectors.living, EntitySelectors.excludeOf(player), EntitySelectors.excludeType(EntityMdBall.class)));
						if(trace != null && trace.entityHit != null) {
							MDDamageHelper.attack(trace.entityHit, player, getDamage(aData));
						}
						actionClient(player, player, ball);
					}
					
				});
				world.spawnEntityInWorld(ball);
			}
			
			aData.addSkillExp(instance, instance.getFloat("expincr"));
			Cooldown.setCooldown(instance, instance.getCooldown(aData));
		}
		
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void actionClient(@RangedTarget(range = 20) EntityPlayer _player, @Instance EntityPlayer player, @Instance EntityMdBall ball) {
		Vec3 dest = getDest(player);
		spawnRay(player.worldObj, player, ball.posX, ball.posY, ball.posZ,
			dest.xCoord, dest.yCoord, dest.zCoord);
	}
	
	@SideOnly(Side.CLIENT)
	private static void spawnRay(World world, EntityPlayer player, double x0, double y0, double z0, double x1, double y1, double z1) {
		EntityMdRaySmall raySmall = new EntityMdRaySmall(world);
		raySmall.setFromTo(x0, y0 + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), z0,
				x1, y1, z1);
		raySmall.viewOptimize = false;
		world.spawnEntityInWorld(raySmall);
	}
	
	static Vec3 getDest(EntityPlayer player) {
		return Raytrace.getLookingPos(player, DISTANCE, EntitySelectors.living).getLeft();
	}

}
