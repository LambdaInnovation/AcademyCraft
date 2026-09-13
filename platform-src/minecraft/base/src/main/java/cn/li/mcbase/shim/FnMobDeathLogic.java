package cn.li.mcbase.shim;

import cn.li.mcbase.entity.logic.IMobDeathLogic;
import cn.li.mcbase.entity.IScriptedMob;
import net.minecraft.world.damagesource.DamageSource;
import clojure.lang.IFn;

public class FnMobDeathLogic implements IMobDeathLogic {
    private final IFn fn;
    public FnMobDeathLogic(IFn fn) { this.fn = fn; }
    @Override public void onDie(IScriptedMob mob, DamageSource source) { fn.invoke(mob, source); }
}
