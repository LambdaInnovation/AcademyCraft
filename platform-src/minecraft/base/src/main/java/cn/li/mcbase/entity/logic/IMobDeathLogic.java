package cn.li.mcbase.entity.logic;

import cn.li.mcbase.entity.IScriptedMob;
import net.minecraft.world.damagesource.DamageSource;

public interface IMobDeathLogic {
    void onDie(IScriptedMob mob, DamageSource source);
}
