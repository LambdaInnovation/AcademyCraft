package cn.academy.core.network;

import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

/**
 * Created by Paindar on 2017/2/16.
 */
public class MessageSBEffect implements IMessage
{
    public static class Handler implements IMessageHandler<MessageSBEffect, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MessageSBEffect msg, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                EntityPlayer player=Minecraft.getMinecraft().thePlayer;
                EntityMdRaySmall raySmall  = new EntityMdRaySmall(player.worldObj);
                raySmall.setFromTo(msg.strX, msg.strY,msg.strZ, msg.endX, msg.endY, msg.endZ);
                raySmall.viewOptimize = false;
                player.worldObj.spawnEntityInWorld(raySmall);
            }

            return null;
        }
    }

    Vec3 str,end;
    NBTTagCompound nbt;

    public MessageSBEffect(){}

    public double strX,strY,strZ;
    public double endX,endY,endZ;

    public MessageSBEffect(Vec3 str,Vec3 end) {
        strX=str.xCoord;
        strY=str.yCoord;
        strZ=str.zCoord;
        endX=end.xCoord;
        endY=end.yCoord;
        endZ=end.zCoord;
    }

    /**
     * Convert from the supplied buffer into your specific message type
     *
     * @param buf
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        strX=buf.readDouble();
        strY=buf.readDouble();
        strZ=buf.readDouble();
        endX=buf.readDouble();
        endY=buf.readDouble();
        endZ=buf.readDouble();
    }

    /**
     * Deconstruct your message into the supplied byte buffer
     *
     * @param buf
     */
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(strX);
        buf.writeDouble(strY);
        buf.writeDouble(strZ);
        buf.writeDouble(endX);
        buf.writeDouble(endY);
        buf.writeDouble(endZ);
    }
}
