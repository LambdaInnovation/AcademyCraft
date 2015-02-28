/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.teleport.data;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.teleport.data.LocationData.Location;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This syncs location data. (I hate writing message all the time!!!
 * @author WeathFolD
 */
@RegistrationClass
public class SyncMsg implements IMessage {
	
	List<Location> list;
	
	public SyncMsg(LocationData data) {
		list = new ArrayList<Location>(data.locationList);
	}

	public SyncMsg() {
		list = new ArrayList();
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int n = buf.readByte();
		for(int i = 0; i < n; ++i) {
			list.add(new Location(buf));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(list.size());
		for(int i = 0; i < list.size(); ++i) {
			Location l = list.get(i);
			l.toBuf(buf);
		}
	}
	
	@RegMessageHandler(msg = SyncMsg.class, side = RegMessageHandler.Side.CLIENT)
	public static class Handler implements IMessageHandler<SyncMsg, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(SyncMsg msg, MessageContext ctx) {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			if(player != null) {
				LocationData data = LocationData.get(player);
				data.locationList = msg.list; //Directly replaces.
			}
			return null;
		}
		
	}

}
