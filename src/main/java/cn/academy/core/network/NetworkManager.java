package cn.academy.core.network;

import cn.academy.core.AcademyCraft;
import com.typesafe.config.Config;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.File;

/**
 * Created by Paindar on 2016/8/31.
 */
public class NetworkManager
{
    public static SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel("AcademyCraft");
    private static int nextID = 0;//我不是很确定这儿的正确性，因为这一整块的代码我都是看着1.8的教程敲出来的。。。
    public static void init(FMLPreInitializationEvent event)
    {
        registerMessage(MessageConfig.Handler.class, MessageConfig.class, Side.CLIENT);
    }

    private static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(
            Class<? extends IMessageHandler<REQ, REPLY>> messageHandler, Class<REQ> requestMessageType, Side side)
    {
        instance.registerMessage(messageHandler, requestMessageType, nextID++, side);
    }

    public static void sendTo(Config cfg, EntityPlayerMP player)
    {

        if(!player.getEntityWorld().isRemote)
        {
            MessageConfig msgConfig = new MessageConfig();
            msgConfig.config=cfg;
            instance.sendTo(msgConfig, player);
        }
    }
}
