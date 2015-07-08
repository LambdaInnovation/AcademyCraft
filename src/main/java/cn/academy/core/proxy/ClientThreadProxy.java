 package cn.academy.core.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cn.liutils.util.helper.PlayerData;

public class ClientThreadProxy extends CommonThreadProxy {

    ClientThreadProxy() {
        super("C" + Minecraft.getMinecraft().thePlayer.getCommandSenderName());
    }

    @Override
    public EntityPlayer getThePlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }
}
