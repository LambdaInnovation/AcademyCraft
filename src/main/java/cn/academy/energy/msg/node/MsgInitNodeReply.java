/**
 * 
 */
package cn.academy.energy.msg.node;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import cn.academy.energy.client.gui.GuiNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.SideOnly;

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
	
	@RegMessageHandler(msg = MsgInitNodeReply.class, side = Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgInitNodeReply, IMessage> {

		@Override
		@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
		public IMessage onMessage(MsgInitNodeReply msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if(!(gs instanceof GuiNode))
				return null;
			GuiNode gui = (GuiNode) gs;
			gui.finishInit(msg.suc);
			return null;
		}
		
	}

}
