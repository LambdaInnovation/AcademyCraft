package cn.li.mc262.client.audio;

import cn.li.mcbase.client.audio.PositionalLoopSoundInstance;

import cn.li.mcver.ResourceLocations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active PositionalLoopSoundInstances by an arbitrary caller-chosen
 * key, so ability code (which never holds a Java reference) can start,
 * reposition, and stop a specific loop by key alone.
 */
public final class LoopingSoundRegistry {

    private static final Map<String, PositionalLoopSoundInstance> ACTIVE = new ConcurrentHashMap<>();

    private LoopingSoundRegistry() {}

    private static SoundEvent resolveSoundEvent(String soundId) {
        int sep = soundId.indexOf(':');
        Identifier loc = sep < 0
            ? ResourceLocations.of("minecraft", soundId)
            : ResourceLocations.of(soundId.substring(0, sep), soundId.substring(sep + 1));
        return BuiltInRegistries.SOUND_EVENT.getValue(loc);
    }

    private static void play(String key, PositionalLoopSoundInstance instance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || instance == null) {
            return;
        }
        ACTIVE.put(key, instance);
        mc.getSoundManager().play(instance);
    }

    public static void start(String key, String soundId, float volume, float pitch, double x, double y, double z) {
        stop(key);
        SoundEvent event = resolveSoundEvent(soundId);
        if (event != null) {
            play(key, new PositionalLoopSoundInstance(
                    event, SoundSource.AMBIENT, volume, pitch, x, y, z));
        }
    }

    public static void startFollowingPlayer(String key, String soundId, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        startFollowingPlayer(key, soundId, volume, pitch, mc.player.getUUID().toString());
    }

    public static void startFollowingPlayer(String key, String soundId, float volume, float pitch, Object player) {
        if (player instanceof Player p) {
            startFollowingPlayer(key, soundId, volume, pitch, p.getUUID().toString());
        }
    }

    public static void startFollowingPlayer(
            String key, String soundId, float volume, float pitch, String playerUuid) {
        startFollowingPlayer(key, soundId, volume, pitch, playerUuid, true);
    }

    /**
     * {@code looping = false} plays the clip once and lets it end on its own,
     * while still being tracked by key so stop() can cut it short -- upstream's
     * FollowEntitySound without setLoop().
     */
    public static void startFollowingPlayer(
            String key, String soundId, float volume, float pitch, String playerUuid, boolean looping) {
        stop(key);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || playerUuid == null || playerUuid.isBlank()) {
            return;
        }

        final UUID uuid;
        try {
            uuid = UUID.fromString(playerUuid);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        Player player = mc.level.getPlayerByUUID(uuid);
        SoundEvent event = resolveSoundEvent(soundId);
        if (player != null && event != null) {
            play(key, new PositionalLoopSoundInstance(
                    event, SoundSource.AMBIENT, volume, pitch,
                    player.getX(), player.getY(), player.getZ(), uuid, looping));
        }
    }

    public static void updatePosition(String key, double x, double y, double z) {
        PositionalLoopSoundInstance instance = ACTIVE.get(key);
        if (instance != null) {
            instance.updatePosition(x, y, z);
        }
    }

    public static void stop(String key) {
        PositionalLoopSoundInstance instance = ACTIVE.remove(key);
        if (instance == null) {
            return;
        }
        instance.markStopped();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            SoundManager soundManager = mc.getSoundManager();
            soundManager.stop(instance);
        }
    }

    public static void stopAll() {
        for (String key : ACTIVE.keySet().toArray(new String[0])) {
            stop(key);
        }
    }
}
