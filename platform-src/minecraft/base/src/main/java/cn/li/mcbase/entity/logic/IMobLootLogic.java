package cn.li.mcbase.entity.logic;

import cn.li.mcbase.entity.IScriptedMob;
import net.minecraft.world.damagesource.DamageSource;

public interface IMobLootLogic {
    /**
     * Return true when loot was handled and vanilla {@code dropFromLootTable} should be skipped.
     */
    boolean dropLoot(IScriptedMob mob, DamageSource source, boolean recentHit);
}
