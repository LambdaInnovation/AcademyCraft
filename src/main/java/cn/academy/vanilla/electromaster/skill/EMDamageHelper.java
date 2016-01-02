package cn.academy.vanilla.electromaster.skill;

import cn.academy.core.util.DamageHelper;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.lambdalib.util.generic.RandUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;

public class EMDamageHelper {
    
    /**
     * Attack with a change to generate a high-voltage creeper.
     */
    static void attack(Entity e, EntityPlayer player, float dmg) {
        DamageHelper.attack(e, player, dmg);
        if(e instanceof EntityCreeper) {
            if(RandUtils.nextFloat() < 0.3f) {
                // Set the creeper to be powered
                ((EntityCreeper) e).getDataWatcher().updateObject(17, (byte) 1);
                ModuleAchievements.trigger(player, "electromaster.attack_creeper");
            }
        }
    }
    
}
