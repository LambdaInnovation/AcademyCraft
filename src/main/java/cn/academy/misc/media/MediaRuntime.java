/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.generic.RegistryUtils;
import com.google.common.base.Throwables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraftforge.common.config.Property;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.Source;

import java.lang.reflect.Field;
import java.net.MalformedURLException;

/**
 * @author KSkun
 */
@Registrant
public class MediaRuntime {

    private static SoundSystem sndSystem;
    private static Library sndLibrary;
    private static Property volumeProperty;
    private static boolean init = false;

    public enum PlayState {
        PLAYING, PAUSED, STOPPED
    }

    @RegInitCallback
    public static void __init() {
        volumeProperty = AcademyCraft.config.get("media_player", "volume", 1.0, "Media Player's volume");
    }

    private static void checkInit() {
        if (!init) {
            init = true;

            try {
                Field fSndManager = RegistryUtils.getObfField(SoundHandler.class, "sndManager", "field_147694_f");
                SoundManager sndMgr = (SoundManager) fSndManager.get(Minecraft.getMinecraft().getSoundHandler());

                Field fSndSystem = RegistryUtils.getObfField(SoundManager.class, "sndSystem", "field_148620_e");
                fSndSystem.setAccessible(true);
                sndSystem = (SoundSystem) fSndSystem.get(sndMgr);

                Field fSndLibrary = SoundSystem.class.getDeclaredField("soundLibrary");
                fSndLibrary.setAccessible(true);
                sndLibrary = (Library) fSndLibrary.get(sndSystem);
            } catch(Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }



    private static void newSource(ACMedia media, boolean loop, int x, int y, int z) {
        sndSystem.newStreamingSource(true, media.getID(),
                media.getSource(),
                media.getID() + media.getFilePostfix(),
                loop, x, y, z,
                SoundSystemConfig.ATTENUATION_NONE,
                SoundSystemConfig.getDefaultAttenuation());
    }

    /**
     * Play the media file in game.
     * @param media The media to play.
     */
    public static void playMedia(ACMedia media, boolean loop) {
        checkInit();

        System.out.println(sndLibrary + "," + media);
        if(sndLibrary.getSources() == null ||
                !sndLibrary.getSources().containsKey(media.getID())) {
            newSource(media, loop, 0, 0, 0);
        }
        sndSystem.play(media.getID());
        setMediaVolume(media, getVolume());
    }

    public static float getVolume() {
        return (float) volumeProperty.getDouble();
    }

    public static void setVolume(float volume) {
        volumeProperty.set((double) volume);
    }

    /**
     * Set the volume of given media.
     * @param media The media to set.
     * @param volume The volumn to set. 0.0F to 1.0F.
     */
    public static void setMediaVolume(ACMedia media, float volume) {
        checkInit();
        sndSystem.setVolume(media.getID(), volume);
    }

    /**
     * Get the volume of given media.
     * @param media The media to get.
     * @return The volume of given media. 0.0F to 1.0F.
     */
    public static float getMediaVolume(ACMedia media) {
        checkInit();
        return sndSystem.getVolume(media.getID());
    }

    /**
     * Pause the media in game.
     * @param media The media to pause.
     */
    public static void pauseMedia(ACMedia media) {
        checkInit();
        sndSystem.pause(media.getID());
    }

    /**
     * Stop the media in game.
     * @param media The media to stop.
     */
    public static void stopMedia(ACMedia media) {
        checkInit();
        sndSystem.stop(media.getID());
    }

    /**
     * @return The time played for given media, or -1 if not playing
     */
    public static float getPlayedTime(ACMedia media) {
        checkInit();
        Source src = source(media);
        return src == null ? -1 : src.millisecondsPlayed() / 1000.0f;
    }

    private static Source source(ACMedia media) {
        return sndLibrary.getSource(media.getID());
    }

    public static PlayState getState(ACMedia media) {
        checkInit();
        Source src = source(media);
        if (src == null || src.stopped()) {
            return PlayState.STOPPED;
        } else if (src.playing()) {
            return PlayState.PLAYING;
        } else {
            return PlayState.PAUSED;
        }
    }

    public static String getDisplayTime(int seconds) {
        if (seconds == -1) {
            return "N/A";
        }

        int minutes = seconds / 60, secondsMod = seconds % 60;
        return wrapTime(minutes) + ":" + wrapTime(secondsMod);
    }

    private static String wrapTime(int sec) {
        return (sec < 10 ? "0" : "") + sec;
    }

}
