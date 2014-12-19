package cn.academy.api.data;

import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.EventHandlerServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class AbilityDataEventListener {
	
	public class ForgeEventListener {
		
		@SubscribeEvent
	    public void onEntityConstructing(EntityConstructing event) {
	        if (event.entity instanceof EntityPlayer &&
	        		!AbilityDataMain.hasData((EntityPlayer) event.entity)) {
	        	//AbilityDataMain.register((EntityPlayer) event.entity);
	        }
	    }

	    @SubscribeEvent
	    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
	    	if (event.entity instanceof EntityPlayer) {
	    		EntityPlayer player = (EntityPlayer) event.entity;
	    		if (!event.entity.worldObj.isRemote) {
	    			if (!AbilityDataMain.hasData(player)) {
	    				AbilityDataMain.register(player);
	    			}
	    			EventHandlerServer.resetPlayerSkillData(player);
	    			AbilityDataMain.sync(player);
	    		} else {
	    			//Do nothing. On client side call resetPlayerSkillData only
	    			//after receiving data from server.
	    		}
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
