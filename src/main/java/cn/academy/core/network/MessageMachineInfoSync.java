package cn.academy.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
            TileEntity tile= Minecraft.getMinecraft().world.getTileEntity(new BlockPos(x,y,z));
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