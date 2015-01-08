/**
 * 
 */
package cn.academy.core.block.dev;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Client->Server when player quits gui
 * @author WeathFolD
 */
@RegistrationClass
public class MsgDismount implements IMessage {
	
	int x, y, z;

	public MsgDismount(TileDeveloper td) {
		x = td.xCoord;
		y = td.yCoord;
		z = td.zCoord;
	}

	public MsgDismount() {}
	
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
	
	@RegMessageHandler(msg = MsgDismount.class, side = RegMessageHandler.Side.SERVER)
	public static class Handler implements IMessageHandler<MsgDismount, IMessage> {

		@Override
		public IMessage onMessage(MsgDismount msg, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			TileEntity te = player.worldObj.getTileEntity(msg.x, msg.y, msg.z);
			if(te == null || !(te instanceof TileDeveloper)) {
				AcademyCraftMod.log.error("ERR: Didn't find TileDeveloper instance");
				return null;
			}
			((TileDeveloper)te).userQuit();
			return null;
		}
		
	}

}
