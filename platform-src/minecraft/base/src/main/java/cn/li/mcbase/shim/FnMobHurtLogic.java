package cn.li.mcbase.shim;

import cn.li.mcbase.entity.logic.IMobHurtLogic;
import cn.li.mcbase.entity.IScriptedMob;
import net.minecraft.world.damagesource.DamageSource;
import clojure.lang.IFn;

public class FnMobHurtLogic implements IMobHurtLogic {
    private final IFn fn;
    public FnMobHurtLogic(IFn fn) { this.fn = fn; }
    @Override public float onIncomingDamage(IScriptedMob mob, DamageSource src, float amt) {
        Object r = fn.invoke(mob, src, amt);
        return r instanceof Number ? ((Number) r).floatValue() : Float.NaN;
    }
}
