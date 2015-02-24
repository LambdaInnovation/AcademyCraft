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
