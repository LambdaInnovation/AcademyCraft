package cn.academy.ability.api.event;

import cn.academy.ability.api.data.AbilityData;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public abstract class AbilityEvent extends Event {
    
    public final EntityPlayer player;
    
    public AbilityEvent(EntityPlayer _player) {
        player = _player;
    }
    
    public AbilityData getAbilityData() {
        return AbilityData.get(player);
    }
    
}