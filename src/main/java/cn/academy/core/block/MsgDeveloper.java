/**
 * 
 */
package cn.academy.core.block;

import cn.academy.core.AcademyCraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Server->Client
 * @author WeathFolD
 */
public class MsgDeveloper implements IMessage {
	
	int x, y, z;
	int energy;
	
	public MsgDeveloper(TileDeveloper dev) {
		x = dev.xCoord;
		y = dev.yCoord;
		z = dev.zCoord;
		energy = (int) dev.curEnergy;
	}
	
	public MsgDeveloper() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readShort();
		z = buf.readInt();
		energy = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x).writeShort(y).writeInt(z)
			.writeInt(energy);
	}
	
	public static class Handler implements IMessageHandler<MsgDeveloper, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgDeveloper msg, MessageContext ctx) {
			World world = Minecraft.getMinecraft().theWorld;
			TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
			if(te == null || !(te instanceof TileDeveloper)) {
				AcademyCraftMod.log.error("Didn't find the right instance while synchronizing AbilityDeveloper");
				return null;
			}
			TileDeveloper dev = (TileDeveloper) te;
			dev.curEnergy = msg.energy;
			return null;
		}
		
	}

}
