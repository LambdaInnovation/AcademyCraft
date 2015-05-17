package cn.academy.core.proxy;

import cn.academy.core.util.PlayerData;
import net.minecraft.entity.player.EntityPlayer;

public class ServerThreadProxy extends CommonThreadProxy {

    ServerThreadProxy() {
        super("S");
    }

    @Override
    public EntityPlayer getThePlayer() {
        return null;
    }

	@Override
	public PlayerData regPlayerData(EntityPlayer player) {
		return new PlayerData.Server(player);
	}

}
