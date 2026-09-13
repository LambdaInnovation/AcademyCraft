package cn.li.mcver;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;

/**
 * Version seam for vanilla EnderDragon multipart parent resolution.
 * 1.20.1 keeps {@code net.minecraft.world.entity.boss.EnderDragonPart}.
 */
public final class EnderDragonParts {
    private EnderDragonParts() {
    }

    /**
     * @return the owning dragon when {@code entity} is an {@link EnderDragonPart}, otherwise {@code null}
     */
    public static Entity parentOrNull(Entity entity) {
        if (!(entity instanceof EnderDragonPart part)) {
            return null;
        }
        return part.parentMob;
    }
}
