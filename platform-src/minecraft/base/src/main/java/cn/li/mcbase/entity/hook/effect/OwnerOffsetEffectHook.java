package cn.li.mcbase.entity.hook.effect;

import cn.li.mcbase.entity.IScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class OwnerOffsetEffectHook implements ScriptedEffectHook {
    private static final double DEFAULT_FORWARD = 1.0D;
    private static final double DEFAULT_VERTICAL = 1.1D;

    @Override
    public void onClientTick(Entity raw, ClientLevel level) {
        if (!(raw instanceof IScriptedEffectEntity entity)) {
            return;
        }
        offsetTick(entity);
    }

    @Override
    public void onServerTick(Entity raw, Level level) {
        if (!(raw instanceof IScriptedEffectEntity entity)) {
            return;
        }
        offsetTick(entity);
    }

    private void offsetTick(IScriptedEffectEntity entity) {
        Player owner = entity.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        ScriptedEffectSpec spec = entity.getEffectSpec();
        double forward = spec == null ? DEFAULT_FORWARD : spec.getDoubleParam("forward", DEFAULT_FORWARD);
        double vertical = spec == null ? DEFAULT_VERTICAL : spec.getDoubleParam("vertical", DEFAULT_VERTICAL);
        Vec3 look = owner.getLookAngle().normalize().scale(forward);
        Entity e = (Entity) entity;
        // EntityMdShield/EntityDiamondShield both sit at
        // player.getPositionVector() + lookVec * 1 + (0, 1.1, 0) -- the look
        // vector is three-dimensional there, so the shield rides up and down
        // with the aim. Dropping look.y pinned it to a fixed height and the
        // shield stopped tracking pitch entirely.
        e.setPos(owner.getX() + look.x,
                 owner.getY() + vertical + look.y,
                 owner.getZ() + look.z);
        e.setYRot(owner.getYRot());
        e.setXRot(owner.getXRot());
    }
}
