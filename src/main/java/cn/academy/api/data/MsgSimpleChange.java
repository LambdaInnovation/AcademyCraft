package cn.academy.api.data;

import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class MsgSimpleChange implements IMessage {
	
	private int entityID;
	private float cp;
	private float maxCP;
	private int skillCount;
	private float[] skillExp;
	
	public MsgSimpleChange() {}
	
	public MsgSimpleChange(AbilityData data) {
		entityID = data.getPlayer().getEntityId();
		cp = data.currentCP;
		maxCP = data.maxCP;
		skillCount = data.getSkillCount();
		skillExp = data.skillExps.clone();
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityID = buf.readInt();
		cp = buf.readFloat();
		maxCP = buf.readFloat();

		skillCount = buf.readInt();
		skillExp = new float[skillCount];
		for (int i = 0; i < skillCount; ++i) {
			skillExp[i] = buf.readFloat();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityID);
		buf.writeFloat(cp);
		buf.writeFloat(maxCP);
		
		buf.writeInt(skillCount);
		for (int i = 0; i < skillCount; ++i) {
			buf.writeFloat(skillExp[i]);
		}
	}

	@RegMessageHandler(msg = MsgSimpleChange.class, side = RegMessageHandler.Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgSimpleChange, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgSimpleChange msg, MessageContext ctx) {
			EntityPlayer thePlayer = Minecraft.getMinecraft().thePlayer;
			if (msg.entityID == thePlayer.getEntityId()) {
				//Only sync cp of thePlayer
				AbilityData data = AbilityDataMain.getData(thePlayer);
				data.currentCP = msg.cp;
				
				if (msg.skillCount != data.getSkillCount()) {
					AcademyCraftMod.log.fatal("Invalid ability data message.");
				}
				data.skillExps = msg.skillExp;
			}
			return null;
		}
		
	}
}
