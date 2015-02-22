/**
 * 
 */
package cn.academy.api.ctrl;

import io.netty.buffer.ByteBuf;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Fired client->server, indicating that something not right(Typically opening GUI) in client and server
 * should cancel skills.
 * @author WeathFolD
 */
@RegistrationClass
public class ClientCancelMessage implements IMessage {

	public ClientCancelMessage() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}
	
	@RegMessageHandler(msg = ClientCancelMessage.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<ClientCancelMessage, IMessage> {

		@Override
		public IMessage onMessage(ClientCancelMessage msg, MessageContext ctx) {
			EventHandlerServer.INSTANCE.resetPlayerSkillData(ctx.getServerHandler().playerEntity);
			return null;
		}
		
	}

}
