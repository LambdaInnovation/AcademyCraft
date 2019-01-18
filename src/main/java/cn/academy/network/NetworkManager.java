package cn.academy.network;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.WorldUtils;
import com.typesafe.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/**
 * Created by Paindar on 2016/8/31.
 */
@Deprecated
public class NetworkManager
{
    public static SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel("AcademyCraft");
    private static int nextID = 0;

    @StateEventCallback
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