package cn.academy.ability.api.proxy;

import net.minecraft.client.Minecraft;

public class ClientThreadProxy extends CommonThreadProxy {

    ClientThreadProxy() {
        super("C" + Minecraft.getMinecraft().thePlayer.getCommandSenderName());
    }

}
