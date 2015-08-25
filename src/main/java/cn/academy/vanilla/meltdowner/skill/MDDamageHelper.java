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

import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.util.DamageHelper;
import cn.academy.vanilla.meltdowner.CatMeltDowner;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.RangedTarget;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class MDDamageHelper {
	
	static final String MARKID = "md_marktick";
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new Events());
	}
	
	static void attack(Entity e, EntityPlayer player, float dmg) {
		DamageHelper.attack(e, DamageSource.causePlayerDamage(player), dmg);
		AbilityData aData = AbilityData.get(player);
		if(aData.isSkillLearned(CatMeltDowner.radIntensify)) {
			int marktick = getMarkTick(player);
			setMarkTick(e, marktick = Math.max(60, marktick));
			syncStartMark(e, e, marktick);
		}
	}
	
	static int getMarkTick(Entity player) {
		if(player.getEntityData().hasKey(MARKID))
			return player.getEntityData().getInteger(MARKID);
		else
			return 0;
	}
	
	static void setMarkTick(Entity player, int ticks) {
		player.getEntityData().setInteger(MARKID, ticks);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void syncStartMark(@RangedTarget(range = 10) Entity e, @Instance Entity e2, @Data Integer value) {
		setMarkTick(e2, value);
	}
	
	public static class Events {
		
		@SubscribeEvent
		public void onLivingUpdate(LivingUpdateEvent event) {
			int tick = getMarkTick(event.entity);
			if(tick > 0)
				setMarkTick(event.entity, tick - 1);
		}
		
		@SideOnly(Side.CLIENT)
		@SubscribeEvent
		public void onUpdateClient(LivingUpdateEvent event) {
			Entity e = event.entity;
			if(e.worldObj.isRemote) {
				if(getMarkTick(e) > 0) {
					if(RandUtils.nextFloat() < 0.9f) {
						Vec3 pos = VecUtils.add(VecUtils.vec(e.posX, e.posY + e.getEyeHeight(), e.posZ), VecUtils.random());
						Vec3 vel = VecUtils.multiply(VecUtils.random(), 0.04);
						e.worldObj.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(e.worldObj, pos, vel));
					}
				}
			}
		}
		
		@SubscribeEvent
		public void onLivingAttack(LivingHurtEvent event) {
			if(getMarkTick(event.entityLiving) > 0) {
				event.ammount *= 1.5f;
			}
		}
		
	}
}
