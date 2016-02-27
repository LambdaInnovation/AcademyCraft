package cn.academy.ability.api.event;

import cn.academy.ability.api.Skill;
import cpw.mods.fml.common.eventhandler.Cancelable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired when a skill attack is about to be performed. Cancel to make the damage not happen.
 */
@Cancelable
public class SkillAttackEvent extends AbilityEvent {

    public final Skill skill;
    public final Entity target;

    /**
     * Modifiable while processing event.
     */
    public float damage;

    public SkillAttackEvent(EntityPlayer _player, Skill _skill, Entity _target, float _damage) {
        super(_player);
        skill = _skill;
        target = _target;
        damage = _damage;
    }

}
