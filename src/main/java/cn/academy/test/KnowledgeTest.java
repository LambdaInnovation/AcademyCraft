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
package cn.academy.test;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import cn.academy.knowledge.KnowledgeData;
import cn.academy.knowledge.event.KnowledgeLearnedEvent;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.util3.GenericUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegSubmoduleInit
@RegEventHandler
public class KnowledgeTest {

	public static void init() {
		KnowledgeData.addKnowledges("BurnedToDeath");
		KnowledgeData.addKnowledges("PlayedSomeMusic");
		KnowledgeData.addKnowledges("bubble");
	}
	
	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if(event.entityLiving instanceof EntityPlayer) {
			System.out.println("PlayerDeath is triggered.");
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			if(player.isBurning()) {
				KnowledgeData.get(player).learn("BurnedToDeath");
			}
		}
	}
	
	@SubscribeEvent
	public void onLearned(KnowledgeLearnedEvent event) {
		System.out.println("Learned [" + event.getKnowledge() + "] in " + GenericUtils.getEffectiveSide());
	}
	
}
