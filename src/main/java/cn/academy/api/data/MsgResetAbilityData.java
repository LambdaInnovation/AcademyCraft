/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.api.ability.Category;
import cn.academy.api.event.AbilityEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ctrl.EventHandlerClient;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class MsgResetAbilityData implements IMessage {
	
    private int entityID;
	private NBTTagCompound data;
	
	public MsgResetAbilityData(EntityPlayer player) {
	    entityID = player.getEntityId();
		data = new NBTTagCompound();
		AbilityDataMain.getData(player).saveNBTData(data);
	}

	public MsgResetAbilityData() {}

	@Override
	public void fromBytes(ByteBuf buf) {
	    entityID = buf.readInt();
		data = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
	    buf.writeInt(entityID);
		ByteBufUtils.writeTag(buf, data);

	}
	
	//TODO check if loading this class (containing Minecraft class and EventHandlerClient) on server will cause an error
	@RegMessageHandler(msg = MsgResetAbilityData.class, side = RegMessageHandler.Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgResetAbilityData, IMessage> {
		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgResetAbilityData message, MessageContext ctx) {
			Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(message.entityID);
			if (entity == null || !(entity instanceof EntityPlayer)) return null;
			EntityPlayer player = (EntityPlayer) entity;
			
			if (!AbilityDataMain.hasData(player)) {
				//register is done in onEntityConstructing, so here the data should exist.
				AcademyCraft.log.fatal("Error on setting AbilityData on client.");
			} else {
				AbilityData data = AbilityDataMain.getData(player);
				Category preCat = data.getCategory();
				data.loadNBTData(message.data);
			}
			
			if (player == Minecraft.getMinecraft().thePlayer) {
    			//Call client side ctrl api.
    			EventHandlerClient.resetPlayerSkillData();
    			MinecraftForge.EVENT_BUS.post(new AbilityEvent.ChangeCategory(AbilityDataMain.getData(player)));
			}
			return null;
		}
	}

}
