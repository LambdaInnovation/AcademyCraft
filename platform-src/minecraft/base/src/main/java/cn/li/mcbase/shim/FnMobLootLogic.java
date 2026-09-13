package cn.li.mcbase.shim;

import cn.li.mcbase.entity.logic.IMobLootLogic;
import cn.li.mcbase.entity.IScriptedMob;
import net.minecraft.world.damagesource.DamageSource;
import clojure.lang.IFn;

public class FnMobLootLogic implements IMobLootLogic {
    private final IFn fn;
    public FnMobLootLogic(IFn fn) { this.fn = fn; }
    @Override public boolean dropLoot(IScriptedMob mob, DamageSource source, boolean recentHit) {
        Object r = fn.invoke(mob, source, recentHit);
        return r instanceof Boolean ? (Boolean) r : false;
    }
}
