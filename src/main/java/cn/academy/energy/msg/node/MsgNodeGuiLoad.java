/**
 * 
 */
package cn.academy.energy.msg.node;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.AcademyCraft;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.block.tile.impl.TileNode;
import cn.academy.energy.client.gui.GuiNode;
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
public class MsgNodeGuiLoad implements IMessage {
	
	boolean loaded;
	String channel;

	public MsgNodeGuiLoad(IWirelessNode node) {
		channel = WirelessSystem.getTileChannel(node);
		loaded = channel != null;
	}
	
	public MsgNodeGuiLoad() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		loaded = buf.readBoolean();
		if(loaded)
			channel = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(loaded);
		if(loaded)
			ByteBufUtils.writeUTF8String(buf, channel);
	}
	
	@RegMessageHandler(msg = MsgNodeGuiLoad.class, side = Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgNodeGuiLoad, IMessage> {

		@Override
		@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
		public IMessage onMessage(MsgNodeGuiLoad msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if(!(gs instanceof GuiNode)) {
				return null;
			}
			GuiNode node = (GuiNode) gs;
			node.synced = true;
			node.curChannel = msg.channel;
			return null;
		}
		
	}
	
	public static class Request implements IMessage {
		
		int x, y, z;
		public Request(TileEntity node) {
			x = node.xCoord;
			y = node.yCoord;
			z = node.zCoord;
		}
		
		public Request() {}

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
		
	}
	
	@RegMessageHandler(msg = Request.class, side = Side.SERVER)
	public static class ReqHandler implements IMessageHandler<Request, MsgNodeGuiLoad> {

		@Override
		public MsgNodeGuiLoad onMessage(Request msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(!(te instanceof TileNode)) {
				AcademyCraft.log.error("Didn't find TileNode in server when opening gui");
				return null;
			}
			TileNode node = (TileNode) te;
			return new MsgNodeGuiLoad(node);
		}
		
	}

}
