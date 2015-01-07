package cn.academy.api.ctrl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import cn.academy.core.AcademyCraftMod;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The message sent from server to client to synchronize the SkillStates of other players.
 * Those SkillStates are mainly used to render the skill effects.
 * @author acaly
 *
 */
@RegistrationClass
public class SkillStateMessage implements IMessage {
	public enum Action {
		START,
		UPDATE,
		FINISH,
	}
	
	private Action action;
	
	private int stateID;
	
	/**
	 * The player this skill belongs to.
	 */
	private int playerEntityId;
	
	/**
	 * The name of the SkillState class. Used to initialize the instance on client.
	 */
	private String className;
	
	/**
	 * NBT data, if any, used by the SkillState.
	 */
	private NBTTagCompound nbt;
	
	public SkillStateMessage() {}
	
	public SkillStateMessage(SkillState state, Action action) {
		this.action = action;
		this.stateID = state.stateID;
		this.playerEntityId = state.player.getEntityId();
		Class<? extends SkillState> clazz = state.getClass();
		this.className = clazz.getName();
		this.nbt = new NBTTagCompound();
		state.toNBT(this.nbt);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		action = Action.values()[buf.readInt()];
		stateID = buf.readInt();
		playerEntityId = buf.readInt();
		className = ByteBufUtils.readUTF8String(buf);
		nbt = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(action.ordinal());
		buf.writeInt(stateID);
		buf.writeInt(playerEntityId);
		ByteBufUtils.writeUTF8String(buf, className);
		ByteBufUtils.writeTag(buf, nbt);
	}

	@RegMessageHandler(msg = SkillStateMessage.class, side = RegMessageHandler.Side.CLIENT)
	public static class Handler implements IMessageHandler<SkillStateMessage, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(SkillStateMessage msg, MessageContext ctx) {
			Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(msg.playerEntityId);
			
			//If the player is not in the same dimension.
			if (entity == null) return null;
			//If the player is thePlayer. These skills are directly handled by EventHandlerClient.
			if (entity == Minecraft.getMinecraft().thePlayer) return null;
			
			SkillState ss = null;
			switch (msg.action) {
			case START:
				try {
					
					Class<?> clazz = Class.forName(msg.className);
					
					for (Constructor ctor : clazz.getDeclaredConstructors()) {
						if (ctor.getParameterTypes().length == 1) { 
							if (ctor.getParameterTypes()[0].isAssignableFrom(EntityPlayer.class)) {
								ctor.setAccessible(true);
								ss = (SkillState) ctor.newInstance(entity);
								break;
							}
						}
					}
				} catch (Exception e) {
					AcademyCraftMod.log.error("Cannot find constructor for SKillState. Check the implementation of your SkillState.");
					throw new RuntimeException(e);
				}
				if (ss == null)
					throw new RuntimeException("Cannot find constructor for SKillState. Check the implementation of your SkillState.");
				
				//Start the skill.
				ss.stateID = msg.stateID;
				ss.startSkill();
				break;
				
			case UPDATE:
				ss = SkillStateManager.getStateById((EntityPlayer) entity, msg.stateID);
				if (ss == null) break;
				ss.onUpdate(msg.nbt);
				break;
				
			case FINISH:
				ss = SkillStateManager.getStateById((EntityPlayer) entity, msg.stateID);
				if (ss == null) break;
				ss.finishSkill();
				break;
			}
			return null;
		}
		
	}
}
