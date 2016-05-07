/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.client.Resources;
import cn.academy.misc.media.MediaRuntime.PlayState;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.client.ClientUtils;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

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
    }

    ACMedia lastMedia;
    ACMedia currentMedia;

    public PlayPref playPref = PlayPref.LOOP;

    MediaPlayer() {
        FMLCommonHandler.instance().bus().register(this);
    }

    public void startPlay(ACMedia media) {
        stop();

        MediaRuntime.playMedia(media, playPref == PlayPref.LOOP);
        currentMedia = media;
        lastMedia = media;
    }

    public void pause() {
        if(currentMedia != null) MediaRuntime.pauseMedia(currentMedia);
    }

    public void resume() {
        if(currentMedia != null) MediaRuntime.playMedia(currentMedia, false);
    }

    public void stop() {
        if(currentMedia != null) {
            MediaRuntime.stopMedia(currentMedia);
            currentMedia = null;
        }
    }

    private ACMedia nextMedia() {
        if (currentMedia == null) {
            return null;
        }

        List<ACMedia> medias = playerMedias();

        switch(playPref) {
            case LOOP:
                int index = MediaManager.indexOf(currentMedia);
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

    private List<ACMedia> playerMedias() {
        return MediaData.of(Minecraft.getMinecraft().thePlayer).installedList();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if(!ClientUtils.isPlayerInGame() || event.phase == Phase.START) return;
        if(currentMedia == null || getState() == PlayState.STOPPED) {
            ACMedia next = nextMedia();
            if(next != null) {
                startPlay(next);
            }
        }
    }

    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent event) {
        stop();
    }

    public PlayState getState() {
        if (currentMedia == null) {
            return PlayState.STOPPED;
        }
        return MediaRuntime.getState(currentMedia);
    }
    
}
