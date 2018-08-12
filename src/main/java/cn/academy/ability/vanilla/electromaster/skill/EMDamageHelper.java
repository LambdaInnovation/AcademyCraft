package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.api.AbilityContext;
import cn.academy.achievement.ModuleAchievements;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.network.datasync.DataParameter;

import java.lang.reflect.Field;

class EMDamageHelper {

    public static DataParameter<Boolean> CREEPER_PWOERED = null;
    /**
     * TODO maybe a event will be better.
     * Attack with a change to generate a high-voltage creeper.
     */
    static void attack(AbilityContext ctx, Entity target, float dmg) {
        ctx.attack(target, dmg);
        if(target instanceof EntityCreeper) {
            if(RandUtils.nextFloat() < 0.3f) {
                if(CREEPER_PWOERED==null) {
                    try{
                        Field field = EntityCreeper.class.getDeclaredField("POWERED");
                        field.setAccessible(true);
                        CREEPER_PWOERED = (DataParameter<Boolean>) field.get(EntityCreeper.class);//TODO need Test
                    } catch (NoSuchFieldException | IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                }
                // Set the creeper to be powered
                target.getDataManager().set(CREEPER_PWOERED, true);
                ModuleAchievements.trigger(ctx.player, "electromaster.attack_creeper");
            }
        }
    }
    
}