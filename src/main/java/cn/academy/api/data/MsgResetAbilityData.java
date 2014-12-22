package cn.academy.api.data;

import cn.academy.api.ctrl.EventHandlerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MsgResetAbilityData implements IMessage {
	
	private NBTTagCompound data;
	
	public MsgResetAbilityData(EntityPlayer player) {
		data = new NBTTagCompound();
		AbilityDataMain.getData(player).saveNBTData(data);
	}

	public MsgResetAbilityData() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		data = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, data);

	}
	
	//TODO check if loading this class (containing Minecraft class and EventHandlerClient) on server will cause an error
	public static class Handler implements IMessageHandler<MsgResetAbilityData, IMessage> {
		@Override
		//@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgResetAbilityData message, MessageContext ctx) {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			AbilityDataMain.getData(player).loadNBTData(message.data);
			//Call client side ctrl api.
			EventHandlerClient.resetPlayerSkillData();
			return null;
		}
	}

}
