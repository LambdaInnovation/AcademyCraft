package cn.academy.core.proxy;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.SyncAction;
import cn.academy.core.util.PlayerData;

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
    
    /*
     * Action instance management
     */
    protected long nextActionId = 0;
    protected HashMap<String, SyncAction> actionMap = new HashMap();

    public void registerAction(SyncAction action) {
        if (action.id != null) {
            throw new RuntimeException("action has been registered");
        }
        String id = sideName + "_" + Long.toString(nextActionId++);
        action.id = id;
        actionMap.put(id, action);
    }

    public void registerAction(String id, SyncAction action) {
        if (action.id != null) {
            throw new RuntimeException("action has been registered");
        }
        action.id = id;
        actionMap.put(id, action);
    }

    public void removeAction(SyncAction action) {
        if (action.id == null || 
                actionMap.remove(action.id) == null) {
            throw new RuntimeException("action has not been registered");
        }
    }

    public SyncAction getActionFromId(String id) {
        return actionMap.get(id);
    }
    
    public String getSideId() {
        return sideName;
    }
}
