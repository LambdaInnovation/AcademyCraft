package cn.li.mc262.client.effect;

import cn.li.mc262.entity.ScriptedEffectEntity;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client-local scripted-effect spawn for Minecraft 26.2.
 * Uses public {@link ClientLevel#addEntity(Entity)} (no reflection).
 * Spawned entities use the shared scripted tick and render pipelines.
 */
public final class ScriptedEffectSpawner {
    /**
     * Client-local entity network ids. Server ids are always positive, so
     * counting down from -1 cannot collide with a tracked entity.
     */
    private static final AtomicInteger LOCAL_ENTITY_IDS = new AtomicInteger(-1);

    private ScriptedEffectSpawner() {
    }

    private static boolean addClientEntity(ClientLevel level, ScriptedEffectEntity effect) {
        try {
            int id = LOCAL_ENTITY_IDS.getAndDecrement();
            effect.setId(id);
            level.addEntity(effect);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Compatibility factory retained for existing 26.2 call sites. */
    public static Object create(ClientLevel level, Player player, String effectId) {
        if (level == null || player == null || effectId == null || effectId.isBlank()) {
            return null;
        }
        return ScriptedEffectEntity.create(level, player, effectId);
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
                UUID uuid = UUID.fromString(ownerUuid);
                for (Player p : level.players()) {
                    if (uuid.equals(p.getUUID())) {
                        owner = p;
                        break;
                    }
                }
                if (owner == null) {
                    owner = level.getPlayerInAnyDimension(uuid);
                }
            } catch (IllegalArgumentException ignored) {
                // fall through to local-player fallback
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
