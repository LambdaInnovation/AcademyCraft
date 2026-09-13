package cn.li.mc1211.entity;

import cn.li.mcbase.entity.IScriptedMob;
import cn.li.mcbase.entity.ScriptedEntityLogicRegistry;

import cn.li.mcbase.entity.logic.IMobDeathLogic;
import cn.li.mcbase.entity.logic.IMobHurtLogic;
import cn.li.mcbase.entity.logic.IMobLootLogic;
import cn.li.mcbase.entity.logic.IMobTickLogic;
import cn.li.mcbase.entity.logic.MobLogicBundle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptedMobEntity extends PathfinderMob implements IScriptedMob {
    private final Map<String, Object> scriptData = new ConcurrentHashMap<>();

    public ScriptedMobEntity(EntityType<? extends ScriptedMobEntity> type, Level level) {
        super(type, level);
    }

    public Object getScriptData(String key) {
        return scriptData.get(key);
    }

    public void setScriptData(String key, Object value) {
        scriptData.put(key, value);
    }

    private MobLogicBundle bundle() {
        return ScriptedEntityLogicRegistry.getMobLogic(getType());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        IMobTickLogic tick = bundle().tick;
        if (tick != null) {
            tick.aiStep(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        IMobHurtLogic hurt = bundle().hurt;
        if (hurt == null) {
            return super.hurt(source, amount);
        }
        float next = hurt.onIncomingDamage(this, source, amount);
        if (Float.isNaN(next)) {
            return false;
        }
        return super.hurt(source, next);
    }

    @Override
    public void die(DamageSource source) {
        IMobDeathLogic death = bundle().death;
        if (death != null) {
            death.onDie(this, source);
        }
        super.die(source);
    }

    @Override
    protected void dropFromLootTable(DamageSource source, boolean recentHit) {
        IMobLootLogic loot = bundle().loot;
        if (loot == null || !loot.dropLoot(this, source, recentHit)) {
            super.dropFromLootTable(source, recentHit);
        }
    }
}
