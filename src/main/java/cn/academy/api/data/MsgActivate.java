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
package cn.academy.api.data;

import io.netty.buffer.ByteBuf;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class MsgActivate implements IMessage {

	boolean state;
	
	public MsgActivate(boolean m) {
		state = m;
	}
	
	public MsgActivate() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		state = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(state);
	}

	@RegMessageHandler(msg = MsgActivate.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler implements IMessageHandler<MsgActivate, IMessage> {

		@Override
		public IMessage onMessage(MsgActivate msg, MessageContext ctx) {
			AbilityDataMain.getData(ctx.getServerHandler().playerEntity).setActivated(msg.state);
			return null;
		}
		
	}
	
}
