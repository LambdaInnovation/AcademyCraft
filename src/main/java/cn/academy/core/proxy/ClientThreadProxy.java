 package cn.academy.core.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.core.util.PlayerData;

public class ClientThreadProxy extends CommonThreadProxy {

    ClientThreadProxy() {
        super("C" + Minecraft.getMinecraft().thePlayer.getCommandSenderName());
    }

    @Override
    public EntityPlayer getThePlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

	@Override
	public PlayerData regPlayerData(EntityPlayer player) {
		return new PlayerData.Client(player);
	}

}
