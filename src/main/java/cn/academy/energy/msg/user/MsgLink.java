/**
 * 
 */
package cn.academy.energy.msg.user;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.AcademyCraft;
import cn.academy.core.energy.WirelessSystem;
import cn.liutils.util.DebugUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 */
public class MsgLink implements IMessage {

	int cuser[],
		cnode[];
	
	public MsgLink(TileEntity user, TileEntity node) {
		cuser = new int[] { user.xCoord, user.yCoord, user.zCoord };
		cnode = new int[] { node.xCoord, node.yCoord, node.zCoord };
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		cuser = new int[] { buf.readInt(), buf.readInt(), buf.readInt() };
		cnode = new int[] { buf.readInt(), buf.readInt(), buf.readInt() };
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(cuser[0]).writeInt(cuser[1]).writeInt(cuser[2])
		.writeInt(cnode[0]).writeInt(cnode[1]).writeInt(cnode[2]);
	}
	
	public static class Handler implements IMessageHandler<MsgLink, IMessage> {

		@Override
		public IMessage onMessage(MsgLink msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity tu = world.getTileEntity(msg.cuser[0], msg.cuser[1], msg.cuser[2]);
			TileEntity tn = world.getTileEntity(msg.cnode[0], msg.cnode[1], msg.cnode[2]);
			if(!(tu instanceof IWirelessTile) || !(tn instanceof IWirelessNode)) {
				AcademyCraft.log.error("Can't link the user entity at server side " + DebugUtils.formatArray(msg.cuser));
				return null;
			}
			WirelessSystem.attachTile((IWirelessTile) tu, (IWirelessNode) tn);
			return null;
		}
		
	}

}
