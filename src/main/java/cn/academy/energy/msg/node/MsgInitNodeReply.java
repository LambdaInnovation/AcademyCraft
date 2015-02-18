/**
 * 
 */
package cn.academy.energy.msg.node;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgInitNodeReply implements IMessage {
	
	boolean suc;

	public MsgInitNodeReply(boolean b) {
		suc = b;
	}
	
	public MsgInitNodeReply() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		suc = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(suc);
	}
	
	public static class Handler implements IMessageHandler<MsgInitNodeReply, IMessage> {

		@Override
		public IMessage onMessage(MsgInitNodeReply msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			
			return null;
		}
		
	}

}
