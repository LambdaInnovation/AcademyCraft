package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.AbilityContext;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.ReflectionUtils;
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
                    Field field = ReflectionUtils.getObfField(EntityCreeper.class, "POWERED", "field_184714_b");
                    field.setAccessible(true);
                    try
                    {
                        CREEPER_PWOERED = (DataParameter<Boolean>) field.get(EntityCreeper.class);//TODO need Test
                    } catch (IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }

                }
                // Set the creeper to be powered
                target.getDataManager().set(CREEPER_PWOERED, true);
            }
        }
    }
    
}