/**
 * 
 */
package cn.academy.energy.msg.fr;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import cn.academy.energy.client.gui.GuiFreqRegulator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class MsgFRActionReply implements IMessage {
	
	boolean suc;

	MsgFRActionReply(boolean b) {
		suc = b;
	}
	
	public MsgFRActionReply() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		suc = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(suc);
	}
	
	@RegMessageHandler(msg = MsgFRActionReply.class, side = RegMessageHandler.Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgFRActionReply, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgFRActionReply msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if(!(gs instanceof GuiFreqRegulator))
				return null;
			((GuiFreqRegulator)gs).actionFinish(msg.suc);
			return null;
		}
		
	}

}
