/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.msg;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Force teleports player to some location. Fired in client.
 * @author WeathFolD
 */
@RegistrationClass
public class TeleportMsg implements IMessage {
	
	byte dim;
	float x, y, z;
	
	public TeleportMsg(int _dim, float _x, float _y, float _z) {
		dim = (byte) _dim;
		x = _x;
		y = _y;
		z = _z;
	}

	public TeleportMsg() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		dim = buf.readByte();
		x = buf.readFloat();
		y = buf.readFloat();
		z = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(dim).writeFloat(x).writeFloat(y).writeFloat(z);
	}
	
	public static class Handler<T extends TeleportMsg> implements IMessageHandler<T, IMessage> {

		@Override
		public IMessage onMessage(T msg, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			if(player.worldObj.provider.dimensionId != msg.dim)
				player.travelToDimension(msg.dim);
			player.setPositionAndUpdate(msg.x, msg.y, msg.z);
			return null;
		}
		
	}

}
