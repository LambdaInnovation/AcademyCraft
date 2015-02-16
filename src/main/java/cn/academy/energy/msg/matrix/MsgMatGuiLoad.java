/**
 * 
 */
package cn.academy.energy.msg.matrix;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.academy.energy.client.gui.GuiMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgMatGuiLoad implements IMessage {
	
	boolean init;
	String channel;

	public MsgMatGuiLoad(TileMatrix mat) {
		init = mat.isConnected();
		if(init) {
			channel = mat.getChannel();
		}
	}
	
	public MsgMatGuiLoad() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		init = buf.readBoolean();
		if(init)
			channel = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(init);
		if(init)
			ByteBufUtils.writeUTF8String(buf, channel);
	}
	
	@RegMessageHandler(side = RegMessageHandler.Side.CLIENT, msg = MsgMatGuiLoad.class)
	public static class Handler implements IMessageHandler<MsgMatGuiLoad, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgMatGuiLoad msg, MessageContext ctx) {
			System.out.println("received msg");
			
			GuiScreen scr = Minecraft.getMinecraft().currentScreen;
			if(scr == null || !(scr instanceof GuiMatrix)) {
				return null;
			}
			GuiMatrix mat = (GuiMatrix) scr;
			mat.executeEvent(new GuiMatrix.GuiInit(msg.init, msg.channel));
			
			return null;
		}
		
	}

}
