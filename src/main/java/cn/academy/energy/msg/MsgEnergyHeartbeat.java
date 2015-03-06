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
package cn.academy.energy.msg;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.block.tile.base.TileNodeBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class MsgEnergyHeartbeat implements IMessage {
	
	int x, y, z;
	float energy;
	boolean loaded;

	public MsgEnergyHeartbeat(TileNodeBase node) {
		TileEntity te = (TileEntity) node;
		x = te.xCoord;
		y = te.yCoord;
		z = te.zCoord;
		energy = (float) node.getEnergy();
		loaded = WirelessSystem.isTileRegistered(node);
	}
	
	public MsgEnergyHeartbeat() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		energy = buf.readFloat();
		loaded = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeInt(y).writeInt(z).writeFloat(energy).writeBoolean(loaded);
	}
	
	@RegMessageHandler(msg = MsgEnergyHeartbeat.class, side = Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgEnergyHeartbeat, IMessage> {

		@Override
		@SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
		public IMessage onMessage(MsgEnergyHeartbeat msg, MessageContext ctx) {
			World world = Minecraft.getMinecraft().theWorld;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(te instanceof TileNodeBase) {
				TileNodeBase node = (TileNodeBase) te;
				node.setEnergy(msg.energy);
				node.isLoaded = msg.loaded;
			}
			return null;
		}
		
	}

}
