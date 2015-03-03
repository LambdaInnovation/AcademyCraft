/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.ctrl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cn.academy.api.ability.Category;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillEventType;
import cn.academy.api.ctrl.SkillStateManager;
import cn.academy.api.ctrl.pattern.Pattern;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.api.data.MsgActivate;
import cn.academy.api.event.AbilityEvent;
import cn.academy.api.event.ControlStateEvent;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.api.LIGeneralRegistry;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.api.register.Configurable;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Event handler class in client side. Setup key bindings and sync with server.
 * @author acaly
 *
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
@RegConfigurable
@SideOnly(Side.CLIENT)
public class EventHandlerClient implements IKeyHandler {
	
	/**
	 * The key handler. Use this class in order to store id when creating.
	 * @author acaly
	 *
	 */
	private class KeyHandler implements IKeyHandler {
		
		private int id;
		
		public KeyHandler(int id) {
			this.id = id;
		}
		
		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(tickEnd || !ClientUtils.isPlayerInGame()) return;
			if (presets == null) return; //Haven't got the world id yet.
			if (!isSkillEnabled()) return;
			onEvent(PresetManager.getSkillMapping(id), SkillEventType.RAW_DOWN);
		}

		@Override
		public void onKeyUp(int keyCode, boolean tickEnd) {
			if(tickEnd || !ClientUtils.isPlayerInGame()) return;
			if (presets == null) return; //Haven't got the world id yet.
            if (!isSkillEnabled()) return;
			onEvent(PresetManager.getSkillMapping(id), SkillEventType.RAW_UP);
		}

		@Override
		public void onKeyTick(int keyCode, boolean tickEnd) {}
		
	}

	/**
	 * Class used to handle per skill state.
	 * This is where every event on client side is processed.
	 * @author acaly
	 *
	 */
	private static class SingleSkill {
		
		private int skillId;
		private RawEventHandler reh;
		
		/**
		 * How many ticks before sending a keep-alive message to server.
		 * 0 means not counting.
		 */
		private int tickToKeepAlive = 0;
		
		/**
		 * How many ticks before finish a single click (when double click fails).
		 */
		private int tickToFinishClick = 0;
		
		public SingleSkill(int skillId) {
			this.skillId = skillId;
			//Get the raw handler from INSTANCE.
			this.reh = GenericUtils.assertObj(INSTANCE.rehMap.get(skillId));
		}
		
		/**
		 * Process a skill event.
		 * @param type The event type.
		 * @return True if RAW_TICK is needed after this call.
		 */
		public boolean onEvent(SkillEventType type) {
			switch (type) {
			case RAW_DOWN:
				//Send RAW_DOWN to server and client.
				if(!reh.onEvent(type, reh.getTime(), true)) {
					MinecraftForge.EVENT_BUS.post(new AbilityEvent.AbortControl());
					return false;
				}
				toServer(type);
				//Start to count down tickToKeepAlive.
				tickToKeepAlive = RawEventHandler.KA_INTERVAL;
				//Is a double click still possible?
				if (tickToFinishClick > 0) {
					tickToFinishClick = 0;
					//Only inform client side.
					//On server side RAW_DBLCLK is invoked by EventHandlerServer.
					reh.onEvent(SkillEventType.RAW_DBLCLK, reh.getTime(), true);
				}
				return true;
			case RAW_UP:
				//Send RAW_UP to server and client.
				toServer(type);
				reh.onEvent(type, reh.getTime(), true);
				//Start to wait for a double click.
				tickToFinishClick = RawEventHandler.DBL_DELAY;
				tickToKeepAlive = 0;
				return true;
			case RAW_TICK:
				if (tickToKeepAlive == 1) {
					//Reset counter.
					tickToKeepAlive = RawEventHandler.KA_INTERVAL;
					//Send keep-alive message.
					toServer(SkillEventType.RAW_CLIENT_DOWN);
					//Client still need RAW_TICK_DOWN in this time.
					reh.onEvent(SkillEventType.RAW_TICK_DOWN, reh.getTime(), true);
				} else if (tickToKeepAlive > 1) {
					--tickToKeepAlive;
					//Send RAW_TICK_DOWN to client.
					reh.onEvent(SkillEventType.RAW_TICK_DOWN, reh.getTime(), true);
				}
				if (tickToFinishClick == 1) {
					//Reset counter.
					tickToFinishClick = 0;
					//Inform the server that a double click is failed.
					//This RAW_CLIENT_UP will be converted into RAW_CLICK by EventHandlerServer.
					toServer(SkillEventType.RAW_CLIENT_UP);
					//Also inform the client side.
					reh.onEvent(SkillEventType.RAW_CLICK, reh.getTime(), true);
				} else if (tickToFinishClick > 1) {
					--tickToFinishClick;
					//Send a tick to client.
					//TODO is this really needed?
					reh.onEvent(SkillEventType.RAW_TICK_UP, reh.getTime(), true);
				}
				//If either counter is not 0, we still need RAW_TICK next time.
				return tickToKeepAlive > 0 || tickToFinishClick > 0;
			case RAW_CANCEL:
				//Skill is cancelled. Just log it and inform the client.
				AcademyCraft.log.warn("Skill cancelled by server.");
				reh.onEvent(SkillEventType.RAW_CANCEL, reh.getTime(), true);
				//Reset both counters.
				tickToKeepAlive = tickToFinishClick = 0;
				return false;
			default:
				AcademyCraft.log.error("Unexpected event in EventHandlerClient.");
				return false;
			}
		}
		
		/**
		 * Helper function to sent a message to server.
		 * @param type
		 */
		private void toServer(SkillEventType type) {
			AcademyCraft.netHandler.sendToServer(new ControlMessage(skillId, type, reh.getTime()));
		}
	}
	
	public static final int MAX_KEYS = 4;

	/**
	 * Default key bindings.
	 */
	private static final int DEFAULT_KEY_S1 = LIKeyProcess.MOUSE_LEFT,
							DEFAULT_KEY_S2 = LIKeyProcess.MOUSE_RIGHT,
							DEFAULT_KEY_S3 = Keyboard.KEY_R,
							DEFAULT_KEY_S4 = Keyboard.KEY_F;
	private static final int DEFAULT_KEY_DISABLE = Keyboard.KEY_V;
	
	@Configurable(category = "Control", key = "KEY_S1", defValueInt = DEFAULT_KEY_S1)
	public static int KEY_S1;
	 
	@Configurable(category = "Control", key = "KEY_S2", defValueInt = DEFAULT_KEY_S2)
	public static int KEY_S2;
	
	@Configurable(category = "Control", key = "KEY_S3", defValueInt = DEFAULT_KEY_S3)
	public static int KEY_S3;
	
	@Configurable(category = "Control", key = "KEY_S4", defValueInt = DEFAULT_KEY_S4)
	public static int KEY_S4;
	
	@Configurable(category = "Control", key = "KEY_DISABLE", defValueInt = DEFAULT_KEY_DISABLE)
	public static int KEY_DISABLE;
	
	@RegEventHandler
	public static final EventHandlerClient INSTANCE = new EventHandlerClient();
	
	private Category category;
	
	/**
	 * Handles presets
	 */
	private PresetManager presets;
	
	/**
	 * Make it private.
	 */
	private EventHandlerClient() {}

	/**
	 * Setup the key bindings and network.
	 */
	public static void init() {
		//LIGeneralRegistry.loadConfigurableClass(AcademyCraftMod.config, EventHandlerClient.class);
		
		LIKeyProcess.instance.addKey("Skill 1", KEY_S1, false, INSTANCE.new KeyHandler(0));
		LIKeyProcess.instance.addKey("Skill 2", KEY_S2, false, INSTANCE.new KeyHandler(1));
		LIKeyProcess.instance.addKey("Skill 3", KEY_S3, false, INSTANCE.new KeyHandler(2));
		LIKeyProcess.instance.addKey("Skill 4", KEY_S4, false, INSTANCE.new KeyHandler(3));
		LIKeyProcess.instance.addKey("Ability activation", KEY_DISABLE, false, INSTANCE);
	}
	
	public static PresetManager getPresetManager() {
		return INSTANCE.presets;
	}

	private static void storePresets() {
		LIGeneralRegistry.storeConfigurableClass(AcademyCraft.config, EventHandlerClient.class);
	}
	
	public static int getKeyId(int i) {
		switch(i) {
		case 0: return KEY_S1;
		case 1: return KEY_S2;
		case 2: return KEY_S3;
		case 3: return KEY_S4;
		default:
			throw new RuntimeException("Unsupported Key");
		}
	}
	
	public static void setKey(int i, int keyid) {
		switch(i) {
		case 0: KEY_S1 = keyid; break;
		case 1: KEY_S2 = keyid; break;
		case 2: KEY_S3 = keyid; break;
		case 3: KEY_S4 = keyid; break;
		default:
			throw new RuntimeException("Unsupported Key");
		}
		storePresets();
	}
	
	/**
	 * Called by the data part on client side, after the ability data is prepared.
	 * @param cat The skill data of the player.
	 */
	public static void resetPlayerSkillData() {
		//AcademyCraft.log.info("EventHandlerClient: Reset player.");
		Category cat = AbilityDataMain.getData(Minecraft.getMinecraft().thePlayer).getCategory();

		if(cat == null) {
			AcademyCraft.log.fatal("Get empty category for player on client.");
			return;
		}
		
		if (INSTANCE.presets == null) {
			//First reset in this world. Send a message to server to get the world id before loading the preset.
			AcademyCraft.netHandler.sendToServer(
					new ControlMessage(0, SkillEventType.INIT_QUERY_WORLD_ID, PresetManager.getNextWorldId()));
		} else if (INSTANCE.category != cat) {
			//Category changed! We need to reset PresetManager. Set it null.
			INSTANCE.presets.reset();
		}
		
		INSTANCE.category = cat;

		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		
		Map<Integer, RawEventHandler> rehMap = new HashMap();
		for (int i = 0; i < cat.getSkillCount(); ++i) {
			rehMap.put(i, new RawEventHandler(player, cat.getSkill(i)));
		}
		
		INSTANCE.rehMap = rehMap;
		INSTANCE.kaMap = new HashMap();
	}
	
	/**
	 * Get the world id from server. Use this id to initialize PresetManager.
	 * @param id
	 */
	void loadPresetManager(int id) {
		INSTANCE.presets = new PresetManager(id);
	}
	
	/**
	 * This overrides the open gui event, the player can't open gui when using skills.
	 * @param event
	 */
	@SubscribeEvent
	public void onPlayerOpenGui(GuiOpenEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(player == null) return;
		this.skillEventAll(SkillEventType.RAW_CANCEL);
		AcademyCraft.netHandler.sendToServer(new ControlMessage(0, SkillEventType.CLIENT_STOP_ALL, 0));
	}
	
	@SubscribeEvent
	public void onThePlayerLoggedOut(ClientDisconnectionFromServerEvent event) {
		//First save preset data
		presets.save();
		presets = null;
		
		//Clear in-game objects
		skillEventAll(SkillEventType.RAW_CANCEL);
		kaMap = null;
		rehMap = null;
		category = null;
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		skillEventAll(SkillEventType.RAW_TICK);
		SkillStateManager.tickClient();
	}
	
	void onEvent(int skillId, SkillEventType type) {
		//If it's the empty skill, do nothing.
		if (skillId == 0) return;
		skillEvent(skillId, type);
	}
	
	/**
	 * Raw event handlers. (All created in resetPlayerSkillData.)
	 */
	private Map<Integer, RawEventHandler> rehMap;
	/**
	 * Active skills.
	 */
	private Map<Integer, SingleSkill> kaMap;
	
	/**
	 * Pass the event to the single skill.
	 * @param skillId The skill.
	 * @param type The event type.
	 */
	private void skillEvent(int skillId, SkillEventType type) {
		if (kaMap == null) return; //Not in game
		SingleSkill ka = kaMap.get(skillId);
		if (ka == null) {
			//If the SingleSkill does not exists, create it first.
			ka = new SingleSkill(skillId);
			kaMap.put(skillId, ka);
		}
		
		if (ka.onEvent(type) == false) {
			//The skill is not active. Remove it so that it will not receive RAW_TICK.
			kaMap.remove(skillId);
		}
	}
	
	/**
	 * Pass the event to all active skills.
	 * Used only with RAW_TICK and RAW_CANCEL (when log out and disabled).
	 * @param type The event type
	 */
	private void skillEventAll(SkillEventType type) {
		if (kaMap == null) return; //Not in game
		Iterator<SingleSkill> itor = kaMap.values().iterator();
		while (itor.hasNext()) {
			if (itor.next().onEvent(type) == false) {
				//Remove inactive skills.
				itor.remove();
			}
		}		
	}

	/*
	 * EventHandlerClient as a IKeyHandler, handling KEY_DISABLE.
	 */
	
	public static boolean isSkillEnabled() {
		if(Minecraft.getMinecraft().thePlayer == null)
			return false;
		return AbilityDataMain.getData(Minecraft.getMinecraft().thePlayer).isActivated();
	}
	
	public static boolean isSkillMapped(int sid) {
		return PresetManager.getCurrentPreset().hasSkillMapping(sid);
	}
	
	/**
	 * Manually add cooldown. This should only be used when you are outside any SkilState.
	 */
	public void addCooldown(int skillID) {
		RawEventHandler reh = rehMap.get(skillID);
		if(reh != null) {
			Pattern p = reh.getPattern(0);
			if(p != null)
				p.updateCD();
		}
	}
	
	@Override
	public void onKeyDown(int keyCode, boolean tickEnd) {
		if(tickEnd || !ClientUtils.isPlayerInGame()) return;
		if (presets == null) return;
		
		AbilityData data = AbilityDataMain.getData(Minecraft.getMinecraft().thePlayer);
		boolean enabled = data.isActivated();
		enabled = !enabled;
		
		boolean fakeChange = !enabled && !SkillStateManager.isClientStateEmpty();
		
		if (enabled) {
			//AcademyCraft.log.info("Player skill is enabled.");
		} else {
			//AcademyCraft.log.info("Player skill is disabled.");
			INSTANCE.skillEventAll(SkillEventType.RAW_CANCEL);
			AcademyCraft.netHandler.sendToServer(new ControlMessage(0, SkillEventType.CLIENT_STOP_ALL, 0));
		}
		
		if(fakeChange) { //Using skill. Just clear the states.
			enabled = true;
			MinecraftForge.EVENT_BUS.post(new AbilityEvent.AbortControl()); //Notify.
		} else {
			//Notify
			MinecraftForge.EVENT_BUS.post(new ControlStateEvent());
		}
		
		//change activate and send message!
		if(enabled != data.isActivated()) {
			data.setActivated(enabled);
			AcademyCraft.netHandler.sendToServer(new MsgActivate(enabled));
		}
	}

	@Override
	public void onKeyUp(int keyCode, boolean tickEnd) {}

	@Override
	public void onKeyTick(int keyCode, boolean tickEnd) {}
	
}
