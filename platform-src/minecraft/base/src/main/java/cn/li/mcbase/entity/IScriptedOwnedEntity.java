package cn.li.mcbase.entity;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/** Owned scripted entity surface (effect/marker) shared across MC versions. */
public interface IScriptedOwnedEntity {
    @Nullable
    Player getOwnerPlayer();

    @Nullable
    UUID getOwnerUuid();
}
