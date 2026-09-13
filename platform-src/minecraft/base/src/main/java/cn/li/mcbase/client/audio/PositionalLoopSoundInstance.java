package cn.li.mcbase.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * A native looping sound (this.looping = true) anchored at a position that
 * can be updated every tick and stopped on demand -- used where the ability
 * layer's fire-and-forget sound queue can't express a true continuous loop
 * (e.g. current-charging's held-arc sound, matching upstream's
 * FollowEntitySound(...).setLoop()).
 */
public final class PositionalLoopSoundInstance extends AbstractTickableSoundInstance {

    private volatile boolean stopped = false;
    private final UUID followedPlayerUuid;

    public PositionalLoopSoundInstance(SoundEvent event, SoundSource source, float volume, float pitch,
                                        double x, double y, double z) {
        this(event, source, volume, pitch, x, y, z, null);
    }

    public PositionalLoopSoundInstance(SoundEvent event, SoundSource source, float volume, float pitch,
                                        double x, double y, double z, UUID followedPlayerUuid) {
        this(event, source, volume, pitch, x, y, z, followedPlayerUuid, true);
    }

    /**
     * {@code looping = false} still gives a tracked, repositionable,
     * explicitly stoppable instance -- it just plays its clip once and ends,
     * matching upstream's FollowEntitySound when setLoop() is never called
     * (plasma cannon's 6-second charge clip, cut short by stop()).
     */
    public PositionalLoopSoundInstance(SoundEvent event, SoundSource source, float volume, float pitch,
                                        double x, double y, double z, UUID followedPlayerUuid,
                                        boolean looping) {
        super(event, source, net.minecraft.util.RandomSource.create());
        this.looping = looping;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.followedPlayerUuid = followedPlayerUuid;
    }

    public void updatePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void markStopped() {
        this.stopped = true;
    }

    @Override
    public void tick() {
        if (this.followedPlayerUuid == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Player player = mc.level.getPlayerByUUID(this.followedPlayerUuid);
        if (player != null) {
            updatePosition(player.getX(), player.getY(), player.getZ());
        }
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }
}
