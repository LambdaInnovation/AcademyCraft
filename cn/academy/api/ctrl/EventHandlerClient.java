package cn.academy.api.ctrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import cn.academy.api.ability.Category;
import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.client.key.IKeyHandler;
import cn.liutils.api.util.GenericUtils;
import cn.liutils.core.client.register.LIKeyProcess;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Event handler class in client side. Setup key bindings and sync with server.
 * @author acaly
 *
 */
public class EventHandlerClient {
	
	/**
	 * The key handler. Use this class in order to store id when creating.
	 * @author acaly
	 *
	 */
	private class KeyHandler implements IKeyHandler {
		
		public int id;
		public KeyHandler(int id) {
			this.id = id;
		}
		
		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(tickEnd || !GenericUtils.isPlayerInGame()) return;
			onEvent(presets.getSkillMapping(id), SkillEventType.RAW_DOWN);
		}

		@Override
		public void onKeyUp(int keyCode, boolean tickEnd) {
			if(tickEnd || !GenericUtils.isPlayerInGame()) return;
			onEvent(presets.getSkillMapping(id), SkillEventType.RAW_UP);
		}

		@Override
		public void onKeyTick(int keyCode, boolean tickEnd) {}
		
	}
	
	/**
	 * The network message handler. Use this class to avoid extra instances of EventHandlerClient.
	 * @author acaly
	 *
	 */
	private static class NetworkHandler implements IMessageHandler<ControlMessage, IMessage> {

		@Override
		public IMessage onMessage(ControlMessage msg, MessageContext ctx) {
			if (msg.eventType != SkillEventType.RAW_CANCEL) {
				AcademyCraftMod.log.error("An unexpected packet is received from server.");
			} else {
				INSTANCE.onEvent(msg.skillId, msg.eventType);	
			}
			return null;
		}
		
	}
	
	private static class SkillKeepAlive {
		
		private int skillId;
		private RawEventHandler reh;
		
		private int tickToKeepAlive = 0;
		private int tickToFinishClick = 0;
		
		public SkillKeepAlive(int skillId) {
			this.skillId = skillId;
			this.reh = INSTANCE.rehMap.get(skillId);
		}
		
		public boolean onEvent(SkillEventType type) {
			switch (type) {
			case RAW_DOWN:
				toServer(type);
				reh.onEvent(type, reh.getTime());
				tickToKeepAlive = RawEventHandler.KA_INTERVAL;
				if (tickToFinishClick > 0) {
					tickToFinishClick = 0;
					reh.onEvent(SkillEventType.RAW_DBLCLK, reh.getTime());
				}
				return true;
			case RAW_UP:
				toServer(type);
				reh.onEvent(type, reh.getTime());
				tickToFinishClick = RawEventHandler.DBL_DELAY;
				return true;
			case RAW_TICK:
				if (tickToKeepAlive == 1) {
					tickToKeepAlive = RawEventHandler.KA_INTERVAL;
					toServer(SkillEventType.RAW_CLIENT_DOWN);
					reh.onEvent(SkillEventType.RAW_TICK_DOWN, reh.getTime());
				} else if (tickToKeepAlive > 1) {
					--tickToKeepAlive;
					reh.onEvent(SkillEventType.RAW_TICK_DOWN, reh.getTime());
				}
				if (tickToFinishClick == 1) {
					tickToFinishClick = 0;
					toServer(SkillEventType.RAW_CLIENT_UP);
					reh.onEvent(SkillEventType.RAW_CLICK, reh.getTime());
				} else if (tickToFinishClick > 1) {
					--tickToFinishClick;
					reh.onEvent(SkillEventType.RAW_TICK_UP, reh.getTime());
				}
				return tickToKeepAlive > 0 || tickToFinishClick > 0;
			case RAW_CANCEL:
				AcademyCraftMod.log.warn("Skill cancelled by server.");
				reh.onEvent(SkillEventType.RAW_CANCEL, reh.getTime());
				return false;
			default:
				AcademyCraftMod.log.error("Unexpected event in EventHandlerClient.");
				return false;
			}
		}
		
		private void toServer(SkillEventType type) {
			AcademyCraftMod.netHandler.sendToServer(new ControlMessage(skillId, type, reh.getTime()));
		}
	}

	private static final int DEFAULT_KEY_S1 = LIKeyProcess.MOUSE_LEFT,
							DEFAULT_KEY_S2 = LIKeyProcess.MOUSE_RIGHT,
							DEFAULT_KEY_S3 = Keyboard.KEY_R,
							DEFAULT_KEY_S4 = Keyboard.KEY_F;
	
	private static final EventHandlerClient INSTANCE = new EventHandlerClient();
	
	/**
	 * Handles presets
	 */
	private PresetManager presets;

	/**
	 * Setup the key bindings and network.
	 */
	public static void init() {
		LIKeyProcess.instance.addKey("Skill 1", DEFAULT_KEY_S1, false, INSTANCE.new KeyHandler(0));
		LIKeyProcess.instance.addKey("Skill 2", DEFAULT_KEY_S2, false, INSTANCE.new KeyHandler(1));
		LIKeyProcess.instance.addKey("Skill 3", DEFAULT_KEY_S3, false, INSTANCE.new KeyHandler(2));
		LIKeyProcess.instance.addKey("Skill 4", DEFAULT_KEY_S4, false, INSTANCE.new KeyHandler(3));
		
		AcademyCraftMod.netHandler.registerMessage(NetworkHandler.class, ControlMessage.class, AcademyCraftMod.getNextChannelID(), Side.CLIENT);
	
		FMLCommonHandler.instance().bus().register(INSTANCE);
	}
	
	/**
	 * Called by the data part on client side, after the ability data is prepared.
	 */
	public static void resetPlayerSkillData(Category cat) {
		Map<Integer, RawEventHandler> rehMap = new HashMap();
		for (int i = 0; i < cat.getMaxSkills(); ++i) { //TODO < or <=
			rehMap.put(i, new RawEventHandler(cat.getSkill(i)));
		}
		
		INSTANCE.rehMap = rehMap;
		INSTANCE.kaMap = new HashMap();
		
		//TODO reset PresetManager
		INSTANCE.presets = new PresetManager();
	}

	//TODO onPlayerLoggedOut
	
	public void onEvent(int skillId, SkillEventType type) {
		skillKAEvent(skillId, type);
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		skillKAEventAll(SkillEventType.RAW_TICK);
	}
	
	/*
	 * Internal use.
	 */
	private Map<Integer, RawEventHandler> rehMap;
	private Map<Integer, SkillKeepAlive> kaMap;
	
	private void skillKAEvent(int skillId, SkillEventType type) {
		SkillKeepAlive ka = kaMap.get(skillId);
		if (ka == null) {
			ka = new SkillKeepAlive(skillId);
			kaMap.put(skillId, ka);
		}
		
		if (ka.onEvent(type) == false) {
			kaMap.remove(skillId);
		}
	}
	
	private void skillKAEventAll(SkillEventType type) {
		Iterator<SkillKeepAlive> itor = kaMap.values().iterator();
		while (itor.hasNext()) {
			if (itor.next().onEvent(type) == false) {
				itor.remove();
			}
		}		
	}
	
}
