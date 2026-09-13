package cn.li.mc262.entity;

import cn.li.mcbase.entity.IScriptedMob;
import cn.li.mcbase.entity.ScriptedEntityLogicRegistry;

import cn.li.mcbase.entity.logic.IMobDeathLogic;
import cn.li.mcbase.entity.logic.IMobHurtLogic;
import cn.li.mcbase.entity.logic.IMobLootLogic;
import cn.li.mcbase.entity.logic.IMobTickLogic;
import cn.li.mcbase.entity.logic.MobLogicBundle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        IMobHurtLogic hurt = bundle().hurt;
        if (hurt == null) {
            return super.hurtServer(level, source, amount);
        }
        float next = hurt.onIncomingDamage(this, source, amount);
        if (Float.isNaN(next)) {
            return false;
        }
        return super.hurtServer(level, source, next);
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
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean recentHit) {
        IMobLootLogic loot = bundle().loot;
        if (loot == null || !loot.dropLoot(this, source, recentHit)) {
            super.dropFromLootTable(level, source, recentHit);
        }
    }
}
