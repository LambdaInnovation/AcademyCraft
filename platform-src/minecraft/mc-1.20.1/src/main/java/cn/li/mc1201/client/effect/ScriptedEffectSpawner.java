package cn.li.mc1201.client.effect;

import cn.li.mc1201.entity.ScriptedEffectEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScriptedEffectSpawner {
    /**
     * Client-local entity network ids. {@code ClientLevel} indexes entities by
     * the id the server assigned them, and server ids are always positive, so
     * counting down from -1 can never collide with a tracked entity.
     */
    private static final AtomicInteger LOCAL_ENTITY_IDS = new AtomicInteger(-1);

    private ScriptedEffectSpawner() {
    }

    /**
     * Add a purely client-side entity to the client level.
     *
     * {@code Level#addFreshEntity} must NOT be used here: it is
     * {@code LevelWriter}'s default implementation, which returns false without
     * doing anything, and only {@code ServerLevel} overrides it. Client-only
     * entities have to go through {@code ClientLevel#putNonPlayerEntity}, the
     * same path vanilla uses for entities arriving over the network.
     */
    private static boolean addClientEntity(ClientLevel level, ScriptedEffectEntity effect) {
        try {
            level.putNonPlayerEntity(LOCAL_ENTITY_IDS.getAndDecrement(), effect);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static String spawnLocalWithUuid(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return null;
        }

        ScriptedEffectEntity effect = ScriptedEffectEntity.create(level, player, effectId);
        if (!addClientEntity(level, effect)) {
            return null;
        }
        return effect.getUUID().toString();
    }

    public static boolean spawnLocal(String effectId) {
        return spawnLocalWithUuid(effectId) != null;
    }

    public static boolean spawnLocalAt(String effectId, double x, double y, double z) {
        if (effectId == null || effectId.isBlank()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return false;
        }

        ScriptedEffectEntity effect = ScriptedEffectEntity.create(level, player, effectId);
        effect.setPos(x, y, z);
        return addClientEntity(level, effect);
    }

    /**
     * Spawn a scripted effect anchored to an arbitrary currently-loaded player
     * (resolved by UUID via the client level's own entity tracking, the same
     * lookup {@link ScriptedEffectEntity#tick()} uses every frame to follow
     * its owner). Falls back to the local player when the target isn't
     * loaded on this client yet (e.g. the very first tick after a nearby
     * player enters render distance) so the effect still spawns somewhere
     * sane rather than being silently dropped.
     *
     * This is what lets a skill's world-visible effect (e.g. a charge glow
     * that must appear at the *caster's* hand for every nearby viewer, not
     * just the caster's own screen) be triggered identically on every
     * recipient's client from a single fanned-out FX message.
     */
    public static String spawnAtPlayerWithUuid(String effectId, String ownerUuid) {
        if (effectId == null || effectId.isBlank()) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }

        Player owner = null;
        if (ownerUuid != null && !ownerUuid.isBlank()) {
            try {
                owner = level.getPlayerByUUID(UUID.fromString(ownerUuid));
            } catch (IllegalArgumentException ignored) {
                // fall through to local-player fallback below
            }
        }
        // Only fall back to the viewer when no owner was named at all. This
        // API exists to anchor an effect to a SPECIFIC player for every nearby
        // client; if that player is not loaded here, drawing the effect on
        // whoever happens to be looking is worse than drawing nothing -- a
        // bystander would see the caster's charge burst on themselves.
        if (owner == null && (ownerUuid == null || ownerUuid.isBlank())) {
            owner = mc.player;
        }
        if (owner == null) {
            return null;
        }

        ScriptedEffectEntity effect = ScriptedEffectEntity.create(level, owner, effectId);
        if (!addClientEntity(level, effect)) {
            return null;
        }
        return effect.getUUID().toString();
    }

    /**
     * Move a client-local scripted effect entity to an absolute position
     * (upstream Flashing localTick: marking.setPosition(dest)).
     */
    public static boolean moveLocalByUuid(String entityUuid, double x, double y, double z) {
        if (entityUuid == null || entityUuid.isBlank()) {
            return false;
        }

        final UUID targetUuid;
        try {
            targetUuid = UUID.fromString(entityUuid);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return false;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ScriptedEffectEntity && targetUuid.equals(entity.getUUID())) {
                entity.setPos(x, y, z);
                return true;
            }
        }
        return false;
    }

    public static boolean removeLocalByUuid(String entityUuid) {
        if (entityUuid == null || entityUuid.isBlank()) {
            return false;
        }

        final UUID targetUuid;
        try {
            targetUuid = UUID.fromString(entityUuid);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return false;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ScriptedEffectEntity && targetUuid.equals(entity.getUUID())) {
                entity.discard();
                return true;
            }
        }
        return false;
    }
}
