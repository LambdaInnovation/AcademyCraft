/**
 * 
 */
package cn.academy.energy.msg.node;

import cn.academy.energy.block.tile.impl.TileNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgInitNode implements IMessage {

	/**
	 * 
	 */
	public MsgInitNode(TileNode node, String ssid) {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see cpw.mods.fml.common.network.simpleimpl.IMessage#fromBytes(io.netty.buffer.ByteBuf)
	 */
	@Override
	public void fromBytes(ByteBuf buf) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see cpw.mods.fml.common.network.simpleimpl.IMessage#toBytes(io.netty.buffer.ByteBuf)
	 */
	@Override
	public void toBytes(ByteBuf buf) {
		// TODO Auto-generated method stub

	}
	
	@RegMessageHandler(msg = MsgInitNode.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<MsgInitNode, IMessage> {
		@Override
		public IMessage onMessage(MsgInitNode message, MessageContext ctx) {
			return null;
		}
	}

}
