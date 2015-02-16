/**
 * 
 */
package cn.academy.energy.msg.matrix;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.AcademyCraft;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.client.gui.GuiMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgInitMatrix implements IMessage {

	int x, y, z;
	String channel;
	
	public MsgInitMatrix(IWirelessNode mat, String chan) {
		TileEntity te = (TileEntity) mat;
		x = te.xCoord;
		y = te.yCoord;
		z = te.zCoord;
		channel = chan;
	}
	
	public MsgInitMatrix() {}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		channel = ByteBufUtils.readUTF8String(buf);
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeInt(y).writeInt(z);
		ByteBufUtils.writeUTF8String(buf, channel);
	}

	@RegMessageHandler(msg = MsgInitMatrix.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<MsgInitMatrix, Reply> {

		@Override
		public Reply onMessage(MsgInitMatrix msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(te instanceof IWirelessNode) {
				if(WirelessSystem.hasNetwork(world, msg.channel)) {
					return new Reply(false);
				}
				WirelessSystem.registerNode((IWirelessNode) te, msg.channel);
				return new Reply(true);
			}
			AcademyCraft.log.error("Invalid node position while init matrix");
			return new Reply(true);
		}
		
	}
	
	public static class Reply implements IMessage {
		
		boolean successful;
		
		public Reply(boolean b) {
			successful = b;
		}
		
		public Reply() {}

		@Override
		public void fromBytes(ByteBuf buf) {
			successful = buf.readBoolean();
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeBoolean(successful);
		}
		
		@RegMessageHandler(msg = Reply.class, side = Side.CLIENT)
		public static class Handler implements IMessageHandler<Reply, IMessage> {

			@Override
			@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
			public IMessage onMessage(Reply msg, MessageContext ctx) {
				//TODO: Check the gui and set the state.
				GuiScreen gui = Minecraft.getMinecraft().currentScreen;
				if(gui instanceof GuiMatrix) {
					GuiMatrix mat = (GuiMatrix) gui;
					//mat.events.add(new GuiMatrix.InitReply(msg.successful));
				} else {
					//Possibly closed by player, ignore
				}
				return null;
			}
			
		}
		
	}
	
}
