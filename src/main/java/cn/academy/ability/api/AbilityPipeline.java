package cn.academy.ability.api;

import cn.academy.ability.SkillDamageSource;
import cn.academy.ability.api.event.CalcEvent;
import cn.academy.ability.api.event.CalcEvent.SkillAttack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Uniform utils handling common actions of skills.
 */
public class AbilityPipeline {

    /**
     * Make the player fire the attack with the given skill. The damage will be re-calculated according to the damage
     *  scale specified globally and skill-locally, and influenced by passive skills.
     * @param target The entity to attack
     * @param player The player starts the attack
     * @param skill  The skill used
     * @param damage The amount of damage applied (raw)
     */
    public static void attack(EntityPlayer player, Skill skill, Entity target, float damage) {
        damage = CalcEvent.calc(new SkillAttack(player, skill, target, damage));

        if (damage > 0) {
            target.attackEntityFrom(new SkillDamageSource(player, skill), damage);
        }
    }

}
