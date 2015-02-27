package cn.academy.api.ctrl;

import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
@RegMessageHandler(msg = ControlMessage.class, side = RegMessageHandler.Side.CLIENT)
public class ControlMessageHandlerClient implements IMessageHandler<ControlMessage, IMessage> {
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(ControlMessage msg, MessageContext ctx) {
		//Client side only receives RAW_CANCEL.
		switch (msg.eventType) {
		case RAW_CANCEL:
			EventHandlerClient.INSTANCE.onEvent(msg.skillId, msg.eventType);
			break;
		case INIT_QUERY_WORLD_ID:
			//Get the worldId.
			//There's a hack that we store the id in time.
			EventHandlerClient.INSTANCE.loadPresetManager(msg.time);
			break;
		default:
			AcademyCraft.log.error("An unexpected packet is received from server.");
		}
		return null;
	}
	
}
