package cn.li.mc1211.entity;


import cn.li.mcbase.entity.IScriptedRayEntity;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.hook.ray.ScriptedRayHooks;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ScriptedRayEntity extends ScriptedEffectEntity implements IScriptedRayEntity {
    public ScriptedRayEntity(EntityType<? extends ScriptedEffectEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ScriptedRaySpec getRaySpec() {
        return ScriptedEntitySpecAccess.getScriptedRaySpec(this.getType());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.level() instanceof ClientLevel clientLevel) {
            ScriptedRaySpec spec = getRaySpec();
            String hookId = spec == null ? "" : spec.getHookId();
            ScriptedRayHooks.resolve(hookId).onClientTick(this, clientLevel);
        }
    }
}
