/**
 * 
 */
package cn.academy.core.block.dev;

import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
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
@RegistrationClass
public class MsgDeveloper implements IMessage {
	
	int x, y, z;
	int energy;
	
	boolean isStimulating;
	int maxStimTimes;
	
	int stimSuccess;
	int stimFailure;
	
	public MsgDeveloper(TileDeveloper tileDeveloper) {
		x = tileDeveloper.xCoord;
		y = tileDeveloper.yCoord;
		z = tileDeveloper.zCoord;
		energy = (int) tileDeveloper.curEnergy;
		isStimulating = tileDeveloper.isStimulating;
		stimSuccess = tileDeveloper.stimSuccess;
		stimFailure = tileDeveloper.stimFailure;
	}
	
	public MsgDeveloper() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readShort();
		z = buf.readInt();
		energy = buf.readInt();
		isStimulating = buf.readBoolean();
		stimSuccess = buf.readByte();
		stimFailure = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x)
			.writeShort(y)
			.writeInt(z)
			.writeInt(energy);
		buf.writeBoolean(isStimulating)
			.writeByte(stimSuccess)
			.writeByte(stimFailure);
	}
	
	@RegMessageHandler(msg = MsgDeveloper.class, side = RegMessageHandler.Side.CLIENT)
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
			dev.isStimulating = msg.isStimulating;
			dev.stimSuccess = msg.stimSuccess;
			dev.stimFailure = msg.stimFailure;
			return null;
		}
		
	}

}
