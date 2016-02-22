/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Backend of GuiMediaPlayer.
 * @author WeAthFolD, KSkun
 */
@Registrant
@SideOnly(Side.CLIENT)
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
    };
    
    List<ACMedia> playerMedias = MediaUtils.getAllMedias();
    ACMedia lastMedia;
    ACMedia mediaInst;
    
    public PlayPref playPref = PlayPref.LOOP;
    
    MediaPlayer() {
        FMLCommonHandler.instance().bus().register(this);
    }
    
    public void startPlay() {
        if(lastMedia == null)
            lastMedia = playerMedias.isEmpty() ? null : playerMedias.get(0);
        if(lastMedia != null)
            startPlay(lastMedia);
    }
    
    public void startPlay(ACMedia media) {
        stop();
        MediaUtils.playMedia(media, false);
        lastMedia = media;
    }
    
    public void startPlay(String id) {
        startPlay(MediaUtils.getMedia(id));
    }
    
    public void updatePlayerMedias(List<ACMedia> medias) {
        this.playerMedias = medias;
    }
    
    public boolean isPlaying() {
        return MediaUtils.isPlaying(mediaInst);
    }
    
    public boolean isPaused() {
        return mediaInst == null ? false : MediaUtils.isPaused(mediaInst);
    }
    
    public float getPlayedTime() {
        return MediaUtils.getPlayedTime(mediaInst);
    }
    
    public void pause() {
        if(mediaInst != null) {
            MediaUtils.pauseMedia(mediaInst);
        }
    }
    
    public void resume() {
        if(mediaInst != null) {
            MediaUtils.playMedia(mediaInst, false);
        }
    }
    
    public void stop() {
        if(mediaInst != null) {
            MediaUtils.stopMedia(mediaInst);
            mediaInst = null;
        }
    }
    
    private ACMedia nextMedia() {
        switch(playPref) {
        case LOOP:
            int index = playerMedias.indexOf(this.mediaInst);
            return playerMedias.get((index + 1) % playerMedias.size());
        case RANDOM:
            return playerMedias.get(RandUtils.rangei(0, playerMedias.size()));
        case SINGLE:
            return null;
        case SINGLE_LOOP:
            return mediaInst;
        default:
            return null;
        }
    }
    
    public ACMedia getPlayingMedia() {
        return mediaInst;
    }
    
/*    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if(!ClientUtils.isPlayerInGame() || event.phase == Phase.START)
            return;
        
            if(mediaInst != null && MediaUtils.isStopped(mediaInst)) {
            ACMedia next = nextMedia();
            if(next != null)
                startPlay(next);
        }
        
        if(mediaInst != null && MediaUtils.isPaused(mediaInst)) {
            MediaUtils.pauseMedia(mediaInst);
        }
    }
    
    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent event) {
        stop();
    }*/
    
}
