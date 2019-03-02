package cn.academy.event.ability;

import cn.academy.ability.Skill;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired in SERVER only, when the specified skill's exp is being ADDED.
 * When change category or sth else the event won't get fired.
 * @author WeAthFolD
 */
public class SkillExpChangedEvent extends AbilityEvent {
    
    public final Skill skill;

    public SkillExpChangedEvent(EntityPlayer _player, Skill _skill) {
        super(_player);
        skill = _skill;
    }

}