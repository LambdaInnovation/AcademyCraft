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
package cn.academy.energy.msg.node;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
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
public class MsgNodeLoadList implements IMessage {
	
	List<String> cns;

	public MsgNodeLoadList(TileNode node) {
		cns = WirelessSystem.getAvailableChannels
			(node.getWorldObj(), 
			node.xCoord, 
			node.yCoord, 
			node.zCoord, 
			node.getTransDistance(), 
		20);
	}
	
	public MsgNodeLoadList() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		cns = new ArrayList();
		int n = buf.readShort();
		while(n-- > 0) {
			cns.add(ByteBufUtils.readUTF8String(buf));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeShort(cns.size());
		for(int i = 0; i < cns.size(); ++i) {
			ByteBufUtils.writeUTF8String(buf, cns.get(i));
		}
	}
	
	@RegMessageHandler(msg = MsgNodeLoadList.class, side = Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgNodeLoadList, IMessage> {

		@Override
		@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
		public IMessage onMessage(MsgNodeLoadList msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if(!(gs instanceof GuiNode))
				return null;
			GuiNode gui = (GuiNode) gs;
			gui.listSynced = true;
			gui.channels = msg.cns;
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
	public static class ReqHandler implements IMessageHandler<Request, MsgNodeLoadList> {

		@Override
		public MsgNodeLoadList onMessage(Request msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(!(te instanceof TileNode)) {
				AcademyCraft.log.error("Didn't find TileNode in server when opening gui");
				return null;
			}
			TileNode node = (TileNode) te;
			return new MsgNodeLoadList(node);
		}
		
	}

}
