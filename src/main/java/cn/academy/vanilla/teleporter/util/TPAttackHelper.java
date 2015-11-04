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
package cn.academy.vanilla.teleporter.util;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.AbilityEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.util.DamageHelper;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cn.lambdalib.networkcall.s11n.StorageOption.Target;
import cn.liutils.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
@Registrant
public class TPAttackHelper {
	
	static final String TPC_ID = "ac_tpcount";
	
	public static void incrTPCount(EntityPlayer player) {
		int i = player.getEntityData().getInteger(TPC_ID) + 1;
		if(i >= 400) {
			ModuleAchievements.trigger(player, "teleporter.mastery");
		}
		player.getEntityData().setInteger(TPC_ID, i);
	}

	/**
	 * You should use this in SERVER only. the critical hit event will be post at client if a critical hit happened.
	 */
	public static void attack(EntityPlayer player, Skill skill, Entity target, float damage) {
		AbilityData aData = AbilityData.get(player);
		// Calculate 3 levels of crit hit
		int chLevel = -1;
		for(int i = 0; i < 3; ++i) {
			float prob = AcademyCraft.pipeline.pipeFloat(
				"ac.teleporter.crit_prob." + i, 0, player);
			if(RandUtils.nextFloat() < prob) {
				float multiply = AcademyCraft.getFloat("teleporter._crithit.incr_" + i);
				damage *= multiply;
				player.addChatComponentMessage(new ChatComponentTranslation("ac.ability.teleporter.crithit", multiply));
				ModuleAchievements.trigger(player, "teleporter.critical_attack");
				chLevel = i;
				break;
			}
		}
		
		// Post event
		if(chLevel != -1) {
			MinecraftForge.EVENT_BUS.post(new TPCritHitEvent(player, target, chLevel));
			postAtClient(player, target, chLevel);
		}
		
		DamageHelper.attack(target, DamageSource.causePlayerDamage(player), damage);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void postAtClient(
		@Target EntityPlayer player, @Instance Entity attackee, @Data Integer level) {
		MinecraftForge.EVENT_BUS.post(new TPCritHitEvent(player, attackee, level));
	}
	
	/**
	 * Fired both client and server when player emits an critical hit.
	 */
	public static class TPCritHitEvent extends AbilityEvent {
		public final int level;
		public final Entity target;
		
		public TPCritHitEvent(EntityPlayer _player, Entity _target, int _level) {
			super(_player);
			level = _level;
			target = _target;
		}
	}
	
}
