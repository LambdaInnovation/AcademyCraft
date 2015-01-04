package cn.academy.api.player;

import cn.academy.core.AcademyCraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MsgControlSync implements IMessage {

	public MsgControlSync(ControlData data) {
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {

	}
	
	public static class Handler implements IMessageHandler<MsgControlSync, IMessage> {

		@Override
		public IMessage onMessage(MsgControlSync message, MessageContext ctx) {
			ControlData data = ControlData.get(Minecraft.getMinecraft().thePlayer);
			if (data != null)
				data.loadNBTData(message.tag);
			return null;
		}
		
	}

}
