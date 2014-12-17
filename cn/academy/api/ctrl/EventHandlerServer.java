package cn.academy.api.ctrl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import cn.academy.api.ability.Category;
import cn.academy.core.AcademyCraftMod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

/**
 * The event handler in server side. It's actually a network handler.
 * Interact with EventHandlerClient through network and sent events to the player's ControlHandler.
 * Note that unlike ControlHandler, this class only has one instance on the server side.
 * @author acaly
 *
 */
public class EventHandlerServer {
	
	private static class NetworkHandler implements IMessageHandler<ControlMessage, IMessage> {

		@Override
		public IMessage onMessage(ControlMessage msg, MessageContext ctx) {
			//TODO check time with time in reh
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			switch (msg.eventType) {
			case RAW_DOWN:
			case RAW_UP:
			case RAW_CLIENT_DOWN:
			case RAW_CLIENT_UP:
				INSTANCE.onEvent(player, msg.skillId, msg.eventType, msg.time);
				break;
			default:
				AcademyCraftMod.log.error("An unexpected packet is received from client.");
			}
			return null;
		}
		
	}
	
	private static class SkillKeepAlive {
		
		private EntityPlayerMP player;
		private int skillId;
		private RawEventHandler reh;
		
		/*
		 * Internal state
		 */
		
		private int tickToSetDead = 0;
		private int tickToFinishClick = 0;
		
		public SkillKeepAlive(EntityPlayer player, int skillId) {
			this.player = (EntityPlayerMP) player;
			this.skillId = skillId;
			this.reh = INSTANCE.rehMap.get(player).get(skillId);
		}
		
		public boolean onEvent(SkillEventType type) {
			return onEvent(type, reh.getTime());
		}
		
		public boolean onEvent(SkillEventType type, int time) {
			switch (type) {
			case RAW_TICK:
				if (tickToSetDead == 1) {
					tickToSetDead = 0;
					//set dead
					setDead();
				} else if (tickToSetDead > 1) {
					--tickToSetDead; 
					reh.onEvent(SkillEventType.RAW_TICK_DOWN, time);
				}
				if (tickToFinishClick == 1) {
					tickToFinishClick = 0;
					setDead();
				} else if (tickToFinishClick > 1) {
					--tickToFinishClick;
				}
				return tickToSetDead > 0 || tickToFinishClick > 0;
			case RAW_DOWN:
				if (tickToSetDead > 0) {
					//Already down
					AcademyCraftMod.log.error("Unexpected RAW_DOWN event.");
					setDead();
				}
				reh.onEvent(type, time);
				tickToSetDead = RawEventHandler.KA_INTERVAL + RawEventHandler.KA_DELAY;
				return true;
			case RAW_UP:
				if (tickToFinishClick > 0) {
					//Already up?
					setDead();
					return false;
				}
				if (tickToSetDead == 0) {
					//Not in the down state
					//Do nothing
					return false;
				}
				tickToSetDead = 0; //Stop waiting for CLIENT_DOWN
				tickToFinishClick = RawEventHandler.DBL_DELAY + RawEventHandler.KA_DELAY;
				reh.onEvent(type, time);
				return true;
			case RAW_CLIENT_DOWN:
				if (tickToSetDead == 0) {
					//Already timeout
					//Do nothing
					return false;
				}
				tickToSetDead = RawEventHandler.KA_INTERVAL;
				//Send to RawEventHandler adjusted time from client
				reh.onEvent(type, time);
				return true;
			case RAW_CLIENT_UP:
				if (tickToFinishClick == 0) {
					//Already timeout
					//Do nothing (setDead has been called).
					return false;
				}
				tickToFinishClick = 0;
				reh.onEvent(SkillEventType.RAW_CLICK, reh.getTime());
				return false;
			default:
				AcademyCraftMod.log.error("Unexpcected event in EventHandlerServer.");
				return false;
			}
		}
		
		private void setDead() {
			AcademyCraftMod.log.warn("Delay in client side. Skill cancelled.");
			int time = reh.getTime();
			reh.onEvent(SkillEventType.RAW_CANCEL, time);
			AcademyCraftMod.netHandler.sendTo(
					new ControlMessage(skillId, SkillEventType.RAW_CANCEL, time),
					player);
		}
		
	}
	
	private static final EventHandlerServer INSTANCE = new EventHandlerServer();

	/**
	 * Setup the key bindings and network.
	 */
	public static void init() {
		//Network registry
		AcademyCraftMod.netHandler.registerMessage(NetworkHandler.class, ControlMessage.class, 
				AcademyCraftMod.getNextChannelID(), Side.SERVER);
		//Server tick event
		FMLCommonHandler.instance().bus().register(INSTANCE);
	}
	
	/**
	 * Called by data part on server side, after the ability data is changed.
	 * @param name The name of player joined
	 */
	public static void resetPlayerSkillData(EntityPlayer player, Category cat) {
		Map<Integer, RawEventHandler> rehMap = new HashMap();
		for (int i = 0; i < cat.getMaxSkills(); ++i) {
			rehMap.put(i, new RawEventHandler());//TODO init
		}
		
		INSTANCE.rehMap.put(player, rehMap);
		INSTANCE.kaMap.put(player, new HashMap());
	}
	
	//TODO onPlayerLoggedOut

	public void onEvent(EntityPlayer player, int skillId, SkillEventType type, int timeForSkill) {
		skillKAEvent(player, skillId, type, timeForSkill);
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		skillKAEventAll(SkillEventType.RAW_TICK);
	}
	
	/*
	 * Internal use only.
	 */
	private Map<EntityPlayer, Map<Integer, RawEventHandler>> rehMap = new HashMap();
	private Map<EntityPlayer, Map<Integer, SkillKeepAlive>> kaMap = new HashMap();
	
	private void skillKAEvent(EntityPlayer player, int skillId, SkillEventType type, int time) {
		Map<Integer, SkillKeepAlive> playerMap = kaMap.get(player);
		SkillKeepAlive ka = playerMap.get(skillId);
		if (ka == null) {
			ka = new SkillKeepAlive(player, skillId);
			playerMap.put(skillId, ka);
		}
		
		if (ka.onEvent(type, time) == false) {
			playerMap.remove(skillId);
		}
	}

	private void skillKAEventAll(EntityPlayer player, SkillEventType type) {
		Map<Integer, SkillKeepAlive> playerMap = kaMap.get(player);
		Iterator<SkillKeepAlive> itor = playerMap.values().iterator();
		while (itor.hasNext()) {
			if (itor.next().onEvent(type) == false) {
				itor.remove();
			}
		}
	}
	
	private void skillKAEventAll(SkillEventType type) {
		for (Map<Integer, SkillKeepAlive> playerMap : kaMap.values()) {
			Iterator<SkillKeepAlive> itor = playerMap.values().iterator();
			while (itor.hasNext()) {
				if (itor.next().onEvent(type) == false) {
					itor.remove();
				}
			}
		}
	}
}
