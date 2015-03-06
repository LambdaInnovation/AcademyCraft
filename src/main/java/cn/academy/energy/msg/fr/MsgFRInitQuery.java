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
package cn.academy.energy.msg.fr;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgFRInitQuery implements IMessage {
	
	int x, y, z;

	public MsgFRInitQuery(IWirelessTile iwt) {
		TileEntity te = (TileEntity) iwt;
		x = te.xCoord;
		y = te.yCoord;
		z = te.zCoord;
	}
	
	public MsgFRInitQuery() {}

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
	
	@RegMessageHandler(msg = MsgFRInitQuery.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler implements IMessageHandler<MsgFRInitQuery, MsgFRInit> {

		@Override
		public MsgFRInit onMessage(MsgFRInitQuery msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(!(te instanceof TileUserBase)) {
				AcademyCraft.log.error("Didn't find a correct TileEntity when init FreqRegulator");
				return null;
			}
			return new MsgFRInit((TileUserBase) te);
		}
		
	}

}
