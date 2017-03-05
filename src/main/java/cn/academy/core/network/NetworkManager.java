package cn.academy.core.network;

import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import com.typesafe.config.Config;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;

import java.util.List;

/**
 * Created by Paindar on 2016/8/31.
 */
public class NetworkManager
{
    public static SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel("AcademyCraft");
    private static int nextID = 0;
    public static void init(FMLPreInitializationEvent event)
    {
        registerMessage(MessageConfig.Handler.class, MessageConfig.class, Side.CLIENT);
        registerMessage(MessageSBEffect.Handler.class, MessageSBEffect.class, Side.CLIENT);
        registerMessage(MessageMachineInfoSync.Handler.class, MessageMachineInfoSync.class, Side.CLIENT);
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

    public static void sendSBEffectToClient(EntityPlayer speller, Vec3 str, Vec3 end)
    {
        List<Entity> list= WorldUtils.getEntities(speller, 25, EntitySelectors.player());
        for(Entity e:list)
        {
            if(!speller.getEntityWorld().isRemote)
            {
                MessageSBEffect msg = new MessageSBEffect(str,end);
                instance.sendTo(msg, (EntityPlayerMP)speller);
            }
            else
                throw new IllegalStateException("Wrong context side!");
        }
    }
}
