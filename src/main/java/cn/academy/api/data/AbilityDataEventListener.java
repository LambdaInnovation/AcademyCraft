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
package cn.academy.api.data;

import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@RegistrationClass
public class AbilityDataEventListener {
	
	@RegEventHandler(RegEventHandler.Bus.Forge)
	public static class ForgeEventListener {
		
		@SubscribeEvent
	    public void onEntityConstructing(EntityConstructing event) {
	        if (event.entity instanceof EntityPlayer &&
	        		!AbilityDataMain.hasData((EntityPlayer) event.entity)) {
	        	AbilityDataMain.register((EntityPlayer) event.entity);
	        }
	    }

	    @SubscribeEvent
	    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
	    	if (event.entity instanceof EntityPlayer) {
	    		EntityPlayer player = (EntityPlayer) event.entity;
	    		if (!event.entity.worldObj.isRemote) {
	    			if (!AbilityDataMain.hasData(player)) {
	    				//register is done in onEntityConstructing, so here the data should exist.
	    				AcademyCraft.log.fatal("Error on getting AbilityData on server.");
	    			}
	    	        AbilityDataMain.resetPlayer(player);

	    	        //Due to a bug of MinecraftForge, we need to send this message
	    	        //again to the player.
	    	        AcademyCraft.netHandler.sendTo(new MsgResetAbilityData(player), (EntityPlayerMP) player);
	    		}
	    	}
	    }
	    
	    @SubscribeEvent
	    public void onLivingFallEvent(LivingFallEvent event) {

	    }
	}

	@RegEventHandler(RegEventHandler.Bus.FML)
	public static class FMLEventListener {
		
		@SubscribeEvent
		public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
		{
			/*if (event.player instanceof EntityPlayer) {
				EntityPlayer player = event.player;
				AbilityData data = AbilityDataMain.getData(player);
				
				if (!event.player.worldObj.isRemote) {
					EventHandlerServer.resetPlayerSkillData(player);	
					AbilityDataMain.sync(player);
				} else { //FIXME: may be some problem
					EventHandlerClient.resetPlayerSkillData();
				}
			}*/
		}
		
		@SubscribeEvent
		public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
		{
		}
		
		@SubscribeEvent
		public void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
			EntityPlayer player = event.player;
			AbilityDataMain.getData(player).onPlayerTick();
		}

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            AbilityDataMain.doDataSync();
        }
	}

}
