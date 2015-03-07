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
package cn.academy.core.block.dev;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Client->Server when player quits gui
 * @author WeathFolD
 */
@RegistrationClass
public class MsgDismount implements IMessage {
	
	int x, y, z;

	public MsgDismount(TileDeveloper td) {
		x = td.xCoord;
		y = td.yCoord;
		z = td.zCoord;
	}

	public MsgDismount() {}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeInt(y).writeInt(z);
	}
	
	@RegMessageHandler(msg = MsgDismount.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler implements IMessageHandler<MsgDismount, IMessage> {

		@Override
		public IMessage onMessage(MsgDismount msg, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			TileEntity te = player.worldObj.getTileEntity(msg.x, msg.y, msg.z);
			if(te == null || !(te instanceof TileDeveloper)) {
				AcademyCraft.log.error("ERR: Didn't find TileDeveloper instance");
				return null;
			}
			//Second pass validation, in case of wrong client data
			TileDeveloper dev = (TileDeveloper) te;
			if(player.equals(dev.getUser()))
				((TileDeveloper)te).userQuit();
			return null;
		}
		
	}

}
