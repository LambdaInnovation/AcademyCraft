/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.client.sound.FollowEntitySound;
import com.google.common.base.Throwables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

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
            Field f3 = SoundSystem.class.getDeclaredField("soundLibrary");
            f3.setAccessible(true);
            sndLibrary = (Library) f3.get(sndSystem);
        } catch(Exception e) {
            throw Throwables.propagate(e);
        }
    }

    static void newSource(ACMedia media, boolean loop, int x, int y, int z) {
        try {
            sndSystem.newStreamingSource(true, media.getId(), media.getFile().toURI().toURL(), media.getFile().getName(), loop, x,
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
        if(!media.getFile().exists()) OnlineMediaManager.INSTANCE.downloadMedia(media);
        if(!sndLibrary.getSources().containsKey(media.getId()))
            newSource(media, loop, 0, 0, 0);
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
     * Get the media from both MediaManager and OnlineMediaManager.
     * @param mediaId The ID of media to get.
     * @return The media.
     */
    public static ACMedia getMedia(String mediaId) {
        if(MediaManager.INSTANCE.getMediaIds().contains(mediaId)) {
            return MediaManager.INSTANCE.getMedia(mediaId);
        } else {
            return OnlineMediaManager.INSTANCE.getMedia(mediaId);
        }
    }

    /**
     * Check if the media is registered.
     * @param mediaId The ID of media to check.
     * @return If the media is registered.
     */
    public static boolean hasMedia(String mediaId) {
        return getMedia(mediaId) != null;
    }

    public static List<ACMedia> getAllMedias() {
        List<ACMedia> list = new ArrayList<ACMedia>();
        list.addAll(MediaManager.INSTANCE.getMedias());
        list.addAll(OnlineMediaManager.INSTANCE.getMedias());
        return list;
    }

    public static List<String> getAllIds() {
        List<String> list = new ArrayList<String>();
        list.addAll(MediaManager.INSTANCE.getMediaIds());
        list.addAll(OnlineMediaManager.INSTANCE.getMediaIds());
        return list;
    }

}
