package cn.academy.ability.api;

import cn.academy.ability.SkillDamageSource;
import cn.academy.ability.api.event.CalcEvent;
import cn.academy.ability.api.event.CalcEvent.SkillAttack;
import cn.academy.ability.api.event.ReflectEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.function.Consumer;

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

    /**
     * Performs a skill attack equivalent to {@link #attack}, but has the chance to be "reflected". (Currently used by
     *  solely Vector Manipulation). When reflected, the attack will NOT be applied and the reflectCallback will be
     *  invoked.
     * @param reflectCallback Will get called once reflection happens.
     */
    public static void attackReflect(EntityPlayer           player,
                                     Skill                  skill,
                                     Entity                 target,
                                     float                  damage,
                                     Runnable               reflectCallback) {
        if (MinecraftForge.EVENT_BUS.post(new ReflectEvent(player, skill, target))) {
            reflectCallback.run();
        } else {
            attack(player, skill, target, damage);
        }
    }

}
