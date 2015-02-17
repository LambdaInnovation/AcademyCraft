/**
 * 
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
