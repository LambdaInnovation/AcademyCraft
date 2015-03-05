/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
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
import cn.academy.energy.client.gui.GuiMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class ResponseChangePwd implements IMessage {
	
	boolean successful;

	public ResponseChangePwd(boolean suc) {
		successful = suc;
	}
	
	public ResponseChangePwd() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		successful = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(successful);
	}
	
	@RegMessageHandler(msg = ResponseChangePwd.class, side = Side.CLIENT)
	public static class Handler implements IMessageHandler<ResponseChangePwd, IMessage> {

		@Override
		@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
		public IMessage onMessage(ResponseChangePwd msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if(!(gs instanceof GuiMatrix))
				return null;
			GuiMatrix gui = (GuiMatrix) gs;
			gui.executeEvent(new GuiMatrix.PwdResponse(msg.successful));
			return null;
		}
		
	}

}
