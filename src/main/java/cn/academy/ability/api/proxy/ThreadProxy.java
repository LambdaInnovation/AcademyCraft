package cn.academy.ability.api.proxy;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.SyncAction;

/**
 * Any side-dependent utility goes here, including basic API functions,
 * skill effects, etc.
 * Implement in {@link CommonThreadProxy} if possible.
 * @author acaly
 *
 */
public interface ThreadProxy {
    
	AbilityData getAbilityData(EntityPlayer player);
	
	/**
	 * Get the action with the given id.
     * When the action does not exist, should return null.
	 * @param id
	 * @return
	 */
	SyncAction getActionFromId(String id);
	
	/**
	 * Give an id to the action and add it to id map.
	 * Called by SyncAction.
	 * @param action
	 */
	void registerAction(SyncAction action);
    
    /**
     * Register an action created by another side.
     * Called by ActionSerializer.
     * @param id
     * @param action
     */
    void registerAction(String id, SyncAction action);
	
	/**
	 * Remove the action from id map.
	 * Called by SyncAction.
	 * @param action
	 */
	void removeAction(SyncAction action);
}
