package cn.academy.api.ctrl;

import org.lwjgl.input.Keyboard;

import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.client.key.IKeyHandler;
import cn.liutils.api.util.GenericUtils;
import cn.liutils.core.client.register.LIKeyProcess;
import net.minecraft.client.Minecraft;
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
			if(tickEnd || !GenericUtils.isPlayerInGame())
				return;
			onKeyHandlerDown(id);
		}

		@Override
		public void onKeyUp(int keyCode, boolean tickEnd) {
			if(tickEnd || !GenericUtils.isPlayerInGame())
				return;
			onKeyHandlerUp(id);
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
			INSTANCE.onNetworkMessage(msg.skillId, msg.eventType);
			return null;
		}
		
	}

	private static final int DEFAULT_KEY_S1 = LIKeyProcess.MOUSE_LEFT,
							DEFAULT_KEY_S2 = LIKeyProcess.MOUSE_RIGHT,
							DEFAULT_KEY_S3 = Keyboard.KEY_R,
							DEFAULT_KEY_S4 = Keyboard.KEY_F;
	
	private static final EventHandlerClient INSTANCE = new EventHandlerClient();
	
	/**
	 * The ControlHandler instance in client side.
	 */
	private ControlHandler controlHandler; //null
	
	/**
	 * 
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
	}
	
	/**
	 * Called by the data part on client side, after the ability data is prepared.
	 */
	public static void onPlayerJoinWorld() {
		//Create the ControlHandler
		if (INSTANCE.controlHandler != null) {
			//Error!
		}
		//TODO make sure Minecraft.getMinecraft().thePlayer is not null
		INSTANCE.controlHandler = new ControlHandler(Minecraft.getMinecraft().thePlayer);
	}
	
	/**
	 * Called by the data part on client side, after the player leaves the world.
	 */
	public static void onPlayerLeaveWorld() {
		
	}
	
	/**
	 * Called by the KeyHandler.
	 */
	private void onKeyHandlerDown(int id) {
		controlHandler.onEvent(presets.getSkillMapping(id), SkillEventType.KEY_DOWN);
		//TODO begin send KEY_PRESS
	}
	
	private void onKeyHandlerUp(int id) {
		controlHandler.onEvent(presets.getSkillMapping(id), SkillEventType.KEY_UP);
	}
	
	/**
	 * Called by the NetworkHandler
	 */
	private void onNetworkMessage(int id, SkillEventType event) {
		
	}
}
