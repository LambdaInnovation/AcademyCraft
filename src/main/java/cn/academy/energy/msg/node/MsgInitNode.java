/**
 * 
 */
package cn.academy.energy.msg.node;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.block.tile.impl.TileNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cn.liutils.util.DebugUtils;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgInitNode implements IMessage {
	
	int x, y, z;
	String ssid;
	String pwd;

	public MsgInitNode(TileNode node, String _ssid, String _pwd) {
		x = node.xCoord;
		y = node.yCoord;
		z = node.zCoord;
		ssid = _ssid;
		pwd = _pwd;
	}
	
	public MsgInitNode() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		ssid = ByteBufUtils.readUTF8String(buf);
		pwd = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeInt(y).writeInt(z);
		ByteBufUtils.writeUTF8String(buf, ssid);
		ByteBufUtils.writeUTF8String(buf, pwd);
	}
	
	@RegMessageHandler(msg = MsgInitNode.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<MsgInitNode, MsgInitNodeReply> {
		@Override
		public MsgInitNodeReply onMessage(MsgInitNode msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(!(te instanceof TileNode)) {
				AcademyCraft.log.error("Unable to find WirelessNode when registering, at " + DebugUtils.formatArray(msg.x, msg.y, msg.z));
				return new MsgInitNodeReply(false);
			}
			TileNode node = (TileNode) te;
			//pass validation
			if(!msg.pwd.equals(WirelessSystem.getPassword(world, msg.pwd))) {
				return new MsgInitNodeReply(false);
			};
			WirelessSystem.registerNode(node, msg.ssid);
			return null;
		}
	}

}
