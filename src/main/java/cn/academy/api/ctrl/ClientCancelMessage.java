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
