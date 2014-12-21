package cn.academy.api.ctrl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import cn.academy.core.AcademyCraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class SkillStateMessage implements IMessage {
	
	private int playerEntityId;
	private String className;
	private NBTTagCompound nbt;
	
	public SkillStateMessage() {}
	
	public SkillStateMessage(SkillState state) {
		this.playerEntityId = state.player.getEntityId();
		Class<? extends SkillState> clazz = state.getClass();
		this.className = clazz.getName();
		this.nbt = new NBTTagCompound();
		state.toNBT(this.nbt);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		playerEntityId = buf.readInt();
		className = ByteBufUtils.readUTF8String(buf);
		nbt = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(playerEntityId);
		ByteBufUtils.writeUTF8String(buf, className);
		ByteBufUtils.writeTag(buf, nbt);
	}

	public static class Handler implements IMessageHandler<SkillStateMessage, IMessage> {

		@Override
		public IMessage onMessage(SkillStateMessage msg, MessageContext ctx) {
			try {
				Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(msg.playerEntityId);
				if (entity == null) return null;
				
				Class<?> clazz = Class.forName(msg.className);
				Constructor ctor = clazz.getConstructor(EntityPlayer.class);
				SkillState ss = (SkillState) ctor.newInstance((EntityPlayer) entity);
				
				ss.startSkill();
			} catch (Exception e) {
				AcademyCraftMod.log.error("Error on creating SkillState. Check the implementation of your SkillState.");
				throw new RuntimeException(e);
			}
			return null;
		}
		
	}
}
