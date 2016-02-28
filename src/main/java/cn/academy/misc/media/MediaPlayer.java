/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.core.util.ACMarkdownRenderer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.client.ClientUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.RegistryUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import paulscode.sound.SoundSystem;
import paulscode.sound.libraries.LibraryJavaSound;

import java.util.ArrayList;
import java.util.List;

/**
 * Backend of GuiMediaPlayer.
 * @author WeAthFolD, KSkun
 */
@Registrant
//@SideOnly(Side.CLIENT)
public class MediaPlayer {
    
    public static final MediaPlayer instance = new MediaPlayer();
    
    public enum PlayPref { 
        SINGLE, SINGLE_LOOP, LOOP, RANDOM; 
        
        ResourceLocation icon;
        
        PlayPref() {
            icon = Resources.getTexture("guis/apps/media_player/" + this.toString().toLowerCase());
        }
        
        public ResourceLocation getIcon() {
            return icon;
        }
    }
    
    List<ACMedia> medias = MediaUtils.getAllMedias();
    ACMedia lastMedia;
    ACMedia currentMedia;
    public PlayPref playPref = PlayPref.LOOP;

    MediaPlayer() {
        FMLCommonHandler.instance().bus().register(this);
    }

    public void startPlay() {
        if(lastMedia == null) {
            lastMedia = medias.isEmpty() ? null : medias.get(0);
        } else {
            startPlay(lastMedia);
        }
    }

    public void startPlay(ACMedia media) {
        stop();

        MediaUtils.playMedia(media, false);
        currentMedia = media;
        lastMedia = media;
    }

    public void startPlay(String id) {
        startPlay(MediaUtils.getMedia(id));
    }

    public void pause() {
        if(currentMedia != null) MediaUtils.pauseMedia(currentMedia);
    }

    public void resume() {
        if(currentMedia != null) MediaUtils.playMedia(currentMedia, false);
    }

    public void stop() {
        if(currentMedia != null) {
            MediaUtils.stopMedia(currentMedia);
            currentMedia = null;
        }
    }

    private ACMedia nextMedia() {
        switch(playPref) {
            case LOOP:
                int index = medias.indexOf(currentMedia);
                return medias.get((index + 1) % medias.size());
            case RANDOM:
                return medias.get(RandUtils.rangei(0, medias.size()));
            case SINGLE:
                return null;
            case SINGLE_LOOP:
                return currentMedia;
            default:
                return null;
        }
    }

    public ACMedia getCurrentMedia() {
        return currentMedia;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if(!ClientUtils.isPlayerInGame() || event.phase == Phase.START) return;
        if(currentMedia == null) {
            ACMedia next = nextMedia();
            if(next != null) startPlay(next);
        }
    }

    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent event) {
        stop();
    }

    public boolean isPlaying() {
        if(currentMedia != null) {
            if(MediaUtils.isPaused(currentMedia)) return false;
            return true;
        } else {
            return false;
        }
    }

    public boolean isPaused() {
        if(currentMedia != null) {
            if(!MediaUtils.isPaused(currentMedia)) return false;
            return true;
        } else {
            return false;
        }
    }

    public boolean isStopped() {
        return currentMedia == null;
    }
    
}
