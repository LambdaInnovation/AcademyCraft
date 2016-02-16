/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

import java.lang.reflect.Field;
import java.net.MalformedURLException;

/**
 * @author KSkun
 */
public class MediaUtils {

    private static SoundSystem sndSystem;
    private static Library sndLibrary;

    static {
        try {
            Field f = Minecraft.getMinecraft().getSoundHandler().getClass().getDeclaredField("sndManager");
            f.setAccessible(true);
            SoundManager sndMgr = (SoundManager) f.get(Minecraft.getMinecraft().getSoundHandler());
            Field f2 = sndMgr.getClass().getDeclaredField("sndSystem");
            f2.setAccessible(true);
            sndSystem = (SoundSystem) f2.get(sndMgr);
            System.out.println(((SoundSystem) sndSystem).getClass().getName());
            Field f3 = SoundSystem.class.getDeclaredField("soundLibrary");
            f3.setAccessible(true);
            sndLibrary = (Library) f3.get(sndSystem);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    static void newSource(ACMedia media, boolean loop, int x, int y, int z) {
        try {
            sndSystem.newSource(true, media.getId(), media.getFile().toURI().toURL(), media.getFile().getName(), loop, x,
                    y, z, SoundSystemConfig.ATTENUATION_NONE, SoundSystemConfig.getDefaultAttenuation());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    static void removeSource(ACMedia media) {
        sndSystem.removeSource(media.getId());
    }

    /**
     * Play the media file in game.
     * @param media The media to play.
     */
    public static void playMedia(ACMedia media, boolean loop) {
        if(!sndLibrary.getSources().containsKey(media.getId())) {
            newSource(media, loop, 0, 0, 0);
        } else {
            sndLibrary.setLooping(media.getId(), loop);
        }
        sndSystem.play(media.getId());
    }

    /**
     * Set the volume of given media.
     * @param media The media to set.
     * @param volume The volumn to set. 0.0F to 1.0F.
     */
    public static void setMediaVolume(ACMedia media, float volume) {
        sndSystem.setVolume(media.getId(), volume);
    }

    /**
     * Get the volume of given media.
     * @param media The media to get.
     * @return The volume of given media. 0.0F to 1.0F.
     */
    public static float getMediaVolume(ACMedia media) {
        return sndSystem.getVolume(media.getId());
    }

    /**
     * Pause the media in game.
     * @param media The media to pause.
     */
    public static void pauseMedia(ACMedia media) {
        sndSystem.pause(media.getId());
    }

    /**
     * Stop the media in game.
     * @param media The media to stop.
     */
    public static void stopMedia(ACMedia media) {
        sndSystem.stop(media.getId());
    }

    /**
     * Set the availability of given media.
     * @param media The media to set.
     * @param availability The availability to set.
     * @param player The player to set.
     */
    public static void setAvailability(ACMedia media, boolean availability, EntityPlayer player) {
        return;
    }

}
