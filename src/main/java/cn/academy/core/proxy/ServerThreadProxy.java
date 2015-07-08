package cn.academy.core.proxy;

import cn.liutils.util.helper.PlayerData;
import net.minecraft.entity.player.EntityPlayer;

public class ServerThreadProxy extends CommonThreadProxy {

    ServerThreadProxy() {
        super("S");
    }

    @Override
    public EntityPlayer getThePlayer() {
        return null;
    }

}
