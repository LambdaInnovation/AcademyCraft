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
package cn.academy.core.block.dev;

import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Client->Server, sent when start developing
 * if id == -1, abort the dev action
 * @author WeathFolD
 */
@RegistrationClass
public class MsgActionStart implements IMessage {
	
	int x, y, z;
	int id, par;

	public MsgActionStart(TileDeveloper td, int id, int par) {
		x = td.xCoord;
		y = td.yCoord;
		z = td.zCoord;
		this.id = id;
		this.par = par;
	}

	public MsgActionStart() {}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		id = buf.readByte();
		par = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeInt(y).writeInt(z)
			.writeByte(id).writeByte(par);
	}
	
	@RegMessageHandler(msg = MsgActionStart.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler implements IMessageHandler<MsgActionStart, IMessage> {

		@Override
		public IMessage onMessage(MsgActionStart msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity td = world.getTileEntity(msg.x, msg.y, msg.z);
			if(td == null || !(td instanceof TileDeveloper)) {
				AcademyCraft.log.error("Didn't find developer while starting developement");
			}
			TileDeveloper dev = (TileDeveloper) td;
			dev.startStimulating(msg.id, msg.par);
			return null;
		}
		
	}

}
