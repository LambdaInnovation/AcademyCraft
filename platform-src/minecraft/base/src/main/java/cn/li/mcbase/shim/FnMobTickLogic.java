package cn.li.mcbase.shim;

import cn.li.mcbase.entity.logic.IMobTickLogic;
import cn.li.mcbase.entity.IScriptedMob;
import clojure.lang.IFn;

public class FnMobTickLogic implements IMobTickLogic {
    private final IFn fn;
    public FnMobTickLogic(IFn fn) { this.fn = fn; }
    @Override public void aiStep(IScriptedMob mob) { fn.invoke(mob); }
}
