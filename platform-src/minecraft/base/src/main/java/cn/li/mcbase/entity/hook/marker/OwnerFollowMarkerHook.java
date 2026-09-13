package cn.li.mcbase.entity.hook.marker;

import cn.li.mcbase.entity.IScriptedOwnedEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class OwnerFollowMarkerHook implements ScriptedMarkerHook {
    @Override
    public void onClientTick(Entity entity, ClientLevel level) {
        follow(entity);
    }

    @Override
    public void onServerTick(Entity entity, Level level) {
        follow(entity);
    }

    private static void follow(Entity entity) {
        if (!(entity instanceof IScriptedOwnedEntity owned)) {
            return;
        }
        Player owner = owned.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        entity.setPos(owner.getX(), owner.getY() + 1.1D, owner.getZ());
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
    }
}
