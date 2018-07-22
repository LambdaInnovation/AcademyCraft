package cn.academy.core.network;

import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
                EntityPlayer player=Minecraft.getMinecraft().player;
                EntityMdRaySmall raySmall  = new EntityMdRaySmall(player.world);
                raySmall.setFromTo(msg.strX, msg.strY,msg.strZ, msg.endX, msg.endY, msg.endZ);
                raySmall.viewOptimize = false;
                player.world.spawnEntity(raySmall);
            }

            return null;
        }
    }

    Vec3d str,end;
    NBTTagCompound nbt;

    public MessageSBEffect(){}

    public double strX,strY,strZ;
    public double endX,endY,endZ;

    public MessageSBEffect(Vec3d str, Vec3d end) {
        strX=str.x;
        strY=str.y;
        strZ=str.z;
        endX=end.x;
        endY=end.y;
        endZ=end.z;
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
