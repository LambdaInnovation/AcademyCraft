/**
 * 
 */
package cn.academy.core.block.dev;

import cn.academy.core.AcademyCraftMod;
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
	
	public static class Handler implements IMessageHandler<MsgActionStart, IMessage> {

		@Override
		public IMessage onMessage(MsgActionStart msg, MessageContext ctx) {
			System.out.println("Ha");
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity td = world.getTileEntity(msg.x, msg.y, msg.z);
			if(td == null || !(td instanceof TileDeveloper)) {
				AcademyCraftMod.log.error("Didn't find developer while starting developement");
				//TODO: Do we need retry?
			}
			TileDeveloper dev = (TileDeveloper) td;
			dev.startStimulating(msg.id, msg.par);
			System.out.println("Synced");
			return null;
		}
		
	}

}
