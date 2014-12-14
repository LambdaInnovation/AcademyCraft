package cn.academy.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.core.AcademyCraftMod;
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
			INSTANCE.onNetworkMessage(ctx.getServerHandler().playerEntity, msg.skillId, msg.eventType);
			return null;
		}
		
	}
	
	private static final EventHandlerServer INSTANCE = new EventHandlerServer();

	/**
	 * Setup the key bindings and network.
	 */
	public static void init() {
		AcademyCraftMod.netHandler.registerMessage(NetworkHandler.class, ControlMessage.class, AcademyCraftMod.getNextChannelID(), Side.SERVER);
	}
	
	/**
	 * Called by data part on server side, after the ability data is prepared.
	 * @param name The name of player joined
	 */
	public static void onPlayerJoinWorld(String name) {
		
	}
	
	/**
	 * Called by data part on server side, after the player is removed from the world.
	 * @param name The name of player joined
	 */
	public static void onPlayerLeaveWorld(String name) {
		
	}
	
	/**
	 * Called by the network message handler.
	 */
	private void onNetworkMessage(EntityPlayer player, int id, SkillEventType event) {
		if (event == SkillEventType.KEY_DOWN) {
			//down
		} else if (event == SkillEventType.KEY_UP) {
			//up
		}
	}
}
