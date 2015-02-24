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
package cn.academy.energy.msg.matrix;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.TileMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.annoreg.mc.RegMessageHandler.Side;
import cn.liutils.util.DebugUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * client->server, query msg when mat is loaded
 * @author WeathFolD
 */
@RegistrationClass
public class MsgGuiLoadQuery implements IMessage {
	
	int x, y, z;

	public MsgGuiLoadQuery(TileMatrix tm) {
		x = tm.xCoord;
		y = tm.yCoord;
		z = tm.zCoord;
	}
	
	public MsgGuiLoadQuery() {}

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
	
	@RegMessageHandler(msg = MsgGuiLoadQuery.class, side = Side.SERVER)
	public static class Handler implements IMessageHandler<MsgGuiLoadQuery, MsgMatGuiLoad> {

		@Override
		public MsgMatGuiLoad onMessage(MsgGuiLoadQuery msg, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			World world = player.worldObj;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(!(te instanceof TileMatrix)) {
				AcademyCraft.log.error("Didn't find the wireless matrix at server side when received gui query, at " 
						+ DebugUtils.formatArray(msg.x, msg.y, msg.z));
				return null;
			}
			return new MsgMatGuiLoad((TileMatrix) te);
		}
		
	}

}
