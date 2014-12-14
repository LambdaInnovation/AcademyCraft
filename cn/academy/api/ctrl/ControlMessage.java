package cn.academy.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * The network message used by EventHandlerClient and EventHandlerServer.
 * @author acaly
 *
 */
public class ControlMessage implements IMessage {
	
	public int skillId;
	public SkillEventType eventType;

	@Override
	public void fromBytes(ByteBuf buf) {
		skillId = buf.readInt();
		eventType = SkillEventType.values()[buf.readInt()];
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(skillId);
		buf.writeInt(eventType.ordinal());
	}

}
