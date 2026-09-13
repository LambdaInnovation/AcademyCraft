package cn.li.mcbase.entity.logic;

import cn.li.mcbase.entity.IScriptedMob;
import net.minecraft.world.damagesource.DamageSource;

public interface IMobHurtLogic {
    /**
     * Return {@link Float#NaN} to cancel hurt entirely; otherwise return (possibly modified) damage.
     */
    float onIncomingDamage(IScriptedMob mob, DamageSource source, float amount);
}
