package cn.li.mcbase.entity.hook.ray;

import cn.li.mcbase.entity.IScriptedRayEntity;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class OwnerFollowRayHook implements ScriptedRayHook {
    private static final double DEFAULT_EYE_OFFSET_Y = 0.1D;

    @Override
    public void onClientTick(Entity raw, ClientLevel level) {
        if (!(raw instanceof IScriptedRayEntity entity)) {
            return;
        }
        Player owner = entity.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        ScriptedRaySpec spec = entity.getRaySpec();
        double eyeOffsetY = spec == null ? DEFAULT_EYE_OFFSET_Y : spec.getDoubleParam("eye-offset-y", DEFAULT_EYE_OFFSET_Y);
        Entity e = (Entity) entity;
        e.setPos(owner.getX(), owner.getEyeY() - eyeOffsetY, owner.getZ());
        e.setYRot(owner.getYRot());
        e.setXRot(owner.getXRot());
    }
}
