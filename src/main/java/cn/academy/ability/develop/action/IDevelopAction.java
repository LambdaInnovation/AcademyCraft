package cn.academy.ability.develop.action;

import cn.academy.ability.develop.IDeveloper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * An IDevelopAction represents a single kind of process to be performed in Developer.
 * You have to provide the stimulation count and the learned callback.
 * @author WeAthFolD
 */
public interface IDevelopAction {
    
    int getStimulations(EntityPlayer player);
    
    /**
     * @param player Target player
     * @param developer The developer that is performing this action
     * @return Whether the action can be REALLY started/performed at the moment.
     * This should be some constraints to player's current state.
     */
    boolean validate(EntityPlayer player, IDeveloper developer);
    
    /**
     * The action performed when really learned the develop type.
     */
    void onLearned(EntityPlayer player);

    
}