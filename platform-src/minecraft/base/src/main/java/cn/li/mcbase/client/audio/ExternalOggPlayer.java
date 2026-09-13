package cn.li.mcbase.client.audio;

import net.minecraft.client.Minecraft;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * External OGG playback used by the AcademyCraft Media Player.
 *
 * Decoding stays off the render thread; OpenAL work is marshalled back onto
 * Minecraft's client thread. A generation token prevents an old decode from
 * replacing a newer selection.
 */
public final class ExternalOggPlayer {

    private static volatile int currentSource = 0;
    private static volatile int currentBuffer = 0;
    private static volatile float currentVolume = 1.0f;
    private static volatile boolean loading = false;
    private static final AtomicLong playbackGeneration = new AtomicLong();

    private ExternalOggPlayer() {}

    public static void play(String filePath, float volume) {
        currentVolume = volume;
        loading = true;
        long generation = playbackGeneration.incrementAndGet();
        Thread decodeThread = new Thread(
            () -> decodeAndPlay(filePath, volume, generation),
            "ac-media-decode"
        );
        decodeThread.setDaemon(true);
        decodeThread.start();
    }

    private static void decodeAndPlay(String filePath, float volume, long generation) {
        try {
            final ShortBuffer pcm;
            final int channels;
            final int sampleRate;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer channelsBuf = stack.mallocInt(1);
                IntBuffer sampleRateBuf = stack.mallocInt(1);
                pcm = STBVorbis.stb_vorbis_decode_filename(filePath, channelsBuf, sampleRateBuf);
                if (pcm == null) {
                    if (generation == playbackGeneration.get()) {
                        loading = false;
                    }
                    return;
                }
                channels = channelsBuf.get(0);
                sampleRate = sampleRateBuf.get(0);
            }
            Minecraft.getInstance().execute(() -> {
                try {
                    if (generation != playbackGeneration.get()) {
                        return;
                    }
                    stopInternal();
                    int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    int buffer = AL10.alGenBuffers();
                    AL10.alBufferData(buffer, format, pcm, sampleRate);
                    int source = AL10.alGenSources();
                    AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
                    AL10.alSourcef(source, AL10.AL_GAIN, volume);
                    AL10.alSourcePlay(source);
                    currentSource = source;
                    currentBuffer = buffer;
                    loading = false;
                } finally {
                    LibCStdlib.free(pcm);
                }
            });
        } catch (Throwable ignored) {
            if (generation == playbackGeneration.get()) {
                loading = false;
            }
        }
    }

    public static void stop() {
        playbackGeneration.incrementAndGet();
        loading = false;
        Minecraft.getInstance().execute(ExternalOggPlayer::stopInternal);
    }

    public static void pause() {
        Minecraft.getInstance().execute(() -> {
            if (currentSource != 0) {
                AL10.alSourcePause(currentSource);
            }
        });
    }

    public static void resume() {
        Minecraft.getInstance().execute(() -> {
            if (currentSource != 0) {
                AL10.alSourcePlay(currentSource);
            }
        });
    }

    private static void stopInternal() {
        if (currentSource != 0) {
            AL10.alSourceStop(currentSource);
            AL10.alDeleteSources(currentSource);
            currentSource = 0;
        }
        if (currentBuffer != 0) {
            AL10.alDeleteBuffers(currentBuffer);
            currentBuffer = 0;
        }
    }

    public static void setVolume(float volume) {
        currentVolume = volume;
        Minecraft.getInstance().execute(() -> {
            if (currentSource != 0) {
                AL10.alSourcef(currentSource, AL10.AL_GAIN, volume);
            }
        });
    }

    public static float getVolume() {
        return currentVolume;
    }

    public static boolean isPlaying() {
        return "playing".equals(getPlaybackState());
    }

    public static String getPlaybackState() {
        if (loading) {
            return "loading";
        }
        int source = currentSource;
        if (source == 0) {
            return "stopped";
        }
        try {
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_PLAYING) {
                return "playing";
            }
            if (state == AL10.AL_PAUSED) {
                return "paused";
            }
            return "stopped";
        } catch (Throwable ignored) {
            return "stopped";
        }
    }

    public static float getElapsedSeconds() {
        int source = currentSource;
        if (source == 0) {
            return 0.0f;
        }
        try {
            return AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET);
        } catch (Throwable ignored) {
            return 0.0f;
        }
    }

    /**
     * Jump to {@code seconds} into the current track.
     *
     * <p>Playback binds the whole decoded track to one static buffer (no
     * queued streaming), so AL_SEC_OFFSET addresses the track itself and is
     * writable as a seek. Upstream has no equivalent: its paulscode backend
     * exposes the play position read-only, which is why its progress bar is
     * display-only.
     */
    public static void seek(float seconds) {
        int source = currentSource;
        if (source == 0) {
            return;
        }
        try {
            AL10.alSourcef(source, AL11.AL_SEC_OFFSET, Math.max(0.0f, seconds));
        } catch (Throwable ignored) {
        }
    }
}
