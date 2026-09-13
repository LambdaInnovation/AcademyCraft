package cn.li.mc1201.entity;

import cn.li.mcbase.entity.IScriptedOwnedEntity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.hook.marker.ScriptedMarkerHooks;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ScriptedMarkerEntity extends ScriptedEffectEntity implements IScriptedOwnedEntity {
    public ScriptedMarkerEntity(EntityType<? extends ScriptedEffectEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ScriptedMarkerSpec getMarkerSpec() {
        return ScriptedEntitySpecAccess.getScriptedMarkerSpec(this.getType());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.level() instanceof ClientLevel clientLevel) {
            ScriptedMarkerSpec spec = getMarkerSpec();
            String hookId = spec == null ? "" : spec.getHookId();
            ScriptedMarkerHooks.resolve(hookId).onClientTick(this, clientLevel);
        }
    }
}
