package cn.li.mcver;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * Cross-version world/entity side effects used by shared Clojure cores.
 */
public final class WorldOps {
    private WorldOps() {
    }

    /**
     * Power a creeper via {@link Creeper#thunderHit} with a visual-only bolt
     * that is never added to the level.
     */
    public static boolean tryPowerCreeper(ServerLevel level, Entity entity) {
        if (!(entity instanceof Creeper creeper)) {
            return false;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return false;
        }
        bolt.setVisualOnly(true);
        bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
        creeper.thunderHit(level, bolt);
        return true;
    }

    public static boolean setArrowBaseDamage(Entity entity, double damage) {
        if (!(entity instanceof AbstractArrow arrow)) {
            return false;
        }
        arrow.setBaseDamage(damage);
        return true;
    }
}
