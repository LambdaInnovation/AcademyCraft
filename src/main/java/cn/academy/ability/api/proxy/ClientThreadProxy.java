package cn.academy.ability.api.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class ClientThreadProxy extends CommonThreadProxy {

    ClientThreadProxy() {
        super("C" + Minecraft.getMinecraft().thePlayer.getCommandSenderName());
    }

    @Override
    public EntityPlayer getThePlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

}
