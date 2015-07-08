package cn.academy.core.proxy;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.liutils.util.helper.PlayerData;

public abstract class CommonThreadProxy implements ThreadProxy {
    
    protected final String sideName;
    
    protected CommonThreadProxy(String sideName) {
        this.sideName = sideName;
    }
    
    public static final String DATA_IDENTIFIER  = "ac_PlayerData";

    public PlayerData getPlayerData(EntityPlayer player) {
    	PlayerData ret = (PlayerData) player.getExtendedProperties(DATA_IDENTIFIER);
		if(ret == null) {
			return regPlayerData(player);
		}
		
		ret.player = player;
		return ret;
    }
    
    public abstract PlayerData regPlayerData(EntityPlayer player);

    public String getSideId() {
        return sideName;
    }
}
