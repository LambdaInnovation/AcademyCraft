package cn.academy.core.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * Created by Paindar on 2017/3/5.
 */
public class MessageMachineInfoSync implements IMessage
{

    public static class Handler implements IMessageHandler<MessageMachineInfoSync, IMessage>
    {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MessageMachineInfoSync msg, MessageContext ctx)
        {
            int x= msg.nbt.getInteger("x"),y=msg.nbt.getInteger("y"),z=msg.nbt.getInteger("z");
            TileEntity tile= Minecraft.getMinecraft().theWorld.getTileEntity(x,y,z);
            tile.readFromNBT(msg.nbt);
            return null;
        }
    }

    NBTTagCompound nbt=new NBTTagCompound();
    public MessageMachineInfoSync(){}

    public MessageMachineInfoSync(TileEntity tile)
    {
        tile.writeToNBT(nbt);
    }
    @Override
    public void fromBytes(ByteBuf buf)
    {
        nbt= ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, nbt);
    }
}
