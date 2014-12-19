package cn.academy.core.events;

import cn.academy.api.ability.Abilities;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class ACEventListener {
	
	public class ForgeEventListener {
		
		@SubscribeEvent
	    public void onEntityConstructing(EntityConstructing event) {
	        if (event.entity instanceof EntityPlayer 
	        		&& AbilityDataMain.getData((EntityPlayer) event.entity) == null) {
	            	AbilityDataMain.register((EntityPlayer) event.entity);
	        }
	    }

	    @SubscribeEvent
	    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
	    	if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
	    		AbilityDataMain.getData((EntityPlayer) event.entity).sync();
	    		}
	    	
	    	if (!event.entity.worldObj.isRemote) {
				EventHandlerServer.resetPlayerSkillData((EntityPlayer) event.entity);	
			} else { //FIXME: may be some problem
				EventHandlerClient.resetPlayerSkillData();
			}
	    }
	    
	    @SubscribeEvent
	    public void onLivingFallEvent(LivingFallEvent event) {

	    }
		
	}
	
	public class FMLEventListener {
		
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
		
		
	}

}
