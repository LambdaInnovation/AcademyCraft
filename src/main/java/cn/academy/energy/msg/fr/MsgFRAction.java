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
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class MsgFRAction implements IMessage {
	
	enum ActionType { UNREG, REG };
	ActionType type;
	int x, y, z;
	int[] coords;
	String pwd;

	public MsgFRAction(TileUserBase tub) {
		x = tub.xCoord;
		y = tub.yCoord;
		z = tub.zCoord;
		type = ActionType.UNREG;
	}
	
	public MsgFRAction(TileUserBase tub, int[] coord, String _pwd) {
		x = tub.xCoord;
		y = tub.yCoord;
		z = tub.zCoord;
		coords = coord;
		pwd = _pwd;
		type = ActionType.REG;
	}
	
	public MsgFRAction() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		type = buf.readBoolean() ? ActionType.REG : ActionType.UNREG;
		if(type == ActionType.REG) {
			coords = new int[] { buf.readInt(), buf.readInt(), buf.readInt() };
			pwd = ByteBufUtils.readUTF8String(buf);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeInt(y).writeInt(z);
		if(type == ActionType.REG) {
			buf.writeBoolean(true);
			buf.writeInt(coords[0]).writeInt(coords[1]).writeInt(coords[2]);
			ByteBufUtils.writeUTF8String(buf, pwd);
		} else {
			buf.writeBoolean(false);
		}
	}
	
	@RegMessageHandler(msg = MsgFRAction.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<MsgFRAction, MsgFRActionReply> {

		@Override
		public MsgFRActionReply onMessage(MsgFRAction msg, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(!(te instanceof TileUserBase)) {
				return new MsgFRActionReply(false);
			}
			TileUserBase tub = (TileUserBase) te;
			if(msg.type == ActionType.UNREG) {
				WirelessSystem.unregisterTile(tub);
				return new MsgFRActionReply(true);
			} else {
				TileEntity te2 = world.getTileEntity(msg.coords[0], msg.coords[1], msg.coords[2]);
				if(!(te2 instanceof IWirelessNode)) {
					System.out.println("fail a " + te2);
					return new MsgFRActionReply(false);
				}
				//Pass validation
				IWirelessNode node = (IWirelessNode) te2;
				String pwd = WirelessSystem.getPassword(world, WirelessSystem.getTileChannel(node));
				if(!msg.pwd.equals(pwd)) {
					System.out.println("fail b " + pwd);
					return new MsgFRActionReply(false);
				}
				//Do it!
				System.out.println("successful!");
				WirelessSystem.attachTile(tub, node);
				return new MsgFRActionReply(true);
			}
		}
		
	}

}
