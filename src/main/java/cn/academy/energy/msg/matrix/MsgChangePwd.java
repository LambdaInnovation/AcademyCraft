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
package cn.academy.energy.msg.matrix;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.World;
import cn.academy.core.energy.WirelessSystem;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgChangePwd implements IMessage {
	
	String channel;
	String opw, npw;

	public MsgChangePwd(String _channel, String oldPwd, String newPwd) {
		channel = _channel;
		opw = oldPwd;
		npw = newPwd;
	}

	public MsgChangePwd() {}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		channel = ByteBufUtils.readUTF8String(buf);
		opw = ByteBufUtils.readUTF8String(buf);
		npw = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, channel);
		ByteBufUtils.writeUTF8String(buf, opw);
		ByteBufUtils.writeUTF8String(buf, npw);
	}
	
	@RegMessageHandler(msg = MsgChangePwd.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<MsgChangePwd, ResponseChangePwd> {

		@Override
		public ResponseChangePwd onMessage(MsgChangePwd msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			if(!WirelessSystem.hasNetwork(world, msg.channel)) {
				return new ResponseChangePwd(false);
			}
			//Pass validation
			if(!WirelessSystem.getPassword(world, msg.channel).equals(msg.opw)) {
				return new ResponseChangePwd(false);
			}
			WirelessSystem.setPassword(world, msg.channel, msg.npw);
			return new ResponseChangePwd(true);
		}
		
	}

}
