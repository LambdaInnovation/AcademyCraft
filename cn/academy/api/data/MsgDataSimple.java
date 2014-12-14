/**
 * 
 */
package cn.academy.api.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class MsgDataSimple implements IMessage {
	
	AbilityData targ;
	int catID, levelID;
	float curCP, maxCP;
	
	public MsgDataSimple(AbilityData data) {
		targ = data;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		catID = buf.readByte();
		levelID = buf.readByte();
		curCP = buf.readFloat();
		maxCP = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(targ.getCategoryID());
		buf.writeByte(targ.getLevelID());
		buf.writeFloat(targ.getCurrentCP());
		buf.writeFloat(targ.getMaxCP());
	}
	
	public class Handler implements IMessageHandler<MsgDataSimple, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgDataSimple message, MessageContext ctx) { //placeholder, things done when decoding
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			AbilityData data = AbilityDataMain.getData(player);
			data.catID = catID;
			data.level = levelID;
			data.currentCP = curCP;
			data.maxCP = maxCP;
			return null;
		}
		
	}

}
