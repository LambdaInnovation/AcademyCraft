/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.media;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.liutils.util.client.ClientUtils;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.RegistryUtils;
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

/**
 * Backend of GuiMediaPlayer.
 * @author WeAthFolD
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
	
	List<Media> playerMedias = new ArrayList();
	Media lastMedia;
	
	public PlayPref playPref = PlayPref.LOOP;
	
	SoundManager soundManager;
	BiMap<ISound, String> playingSounds;
	SoundSystem sndSystem;
	
	MediaInstance mediaInst;
	
	MediaPlayer() {
		FMLCommonHandler.instance().bus().register(this);
	}
	
	public void startPlay() {
		if(lastMedia == null)
			lastMedia = playerMedias.isEmpty() ? null : playerMedias.get(0);
		if(lastMedia != null)
			startPlay(lastMedia);
	}
	
	public void startPlay(Media media) {
		stop();
		
		soundManager = RegistryUtils.getFieldInstance(SoundHandler.class, Minecraft.getMinecraft().getSoundHandler(), "sndManager", "field_147694_f");
		playingSounds = ((HashBiMap<String, ISound>) RegistryUtils.getFieldInstance(SoundManager.class, soundManager, "playingSounds", "field_148629_h")).inverse();
		sndSystem = RegistryUtils.getFieldInstance(SoundManager.class, soundManager, "sndSystem", "field_148620_e");
		
		try {
			MusicTicker musicTicker = RegistryUtils.getFieldInstance(Minecraft.class, Minecraft.getMinecraft(), "mcMusicTicker", "field_147126_aw");
			ISound playing = RegistryUtils.getFieldInstance(MusicTicker.class, musicTicker, "field_147678_c");
			if(playing != null) {
				Minecraft.getMinecraft().getSoundHandler().stopSound(playing);
			}
		} catch(Exception e) {
			AcademyCraft.log.error("Failed to stop vanilla music", e);
		}
		
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		Minecraft.getMinecraft().theWorld.playRecord(null, 
			(int) player.posX, (int) player.posY, (int) player.posZ);
		
		mediaInst = new MediaInstance(media);
		soundManager.sndHandler.playSound(mediaInst);
		mediaInst.mediaUUID = playingSounds.get(mediaInst);
		
		lastMedia = media;
		
		checkMedia();
	}
	
	public void startPlay(String name) {
		startPlay(MediaRegistry.getMedia(name));
	}
	
	public void updatePlayerMedias(List<Media> medias) {
		this.playerMedias = medias;
	}
	
	public boolean isPlaying() {
		return mediaInst != null && !mediaInst.disposed;
	}
	
	public boolean isPaused() {
		return mediaInst == null ? false : mediaInst.isPaused;
	}
	
	public float getPlayedTime() {
		return mediaInst.getPlayTime();
	}
	
	public void pause() {
		checkMedia();
		if(mediaInst != null) {
			sndSystem.pause(mediaInst.mediaUUID);
			mediaInst.isPaused = true;
		}
	}
	
	public void resume() {
		checkMedia();
		if(mediaInst != null) {
			sndSystem.play(mediaInst.mediaUUID);
			mediaInst.isPaused = false;
		}
	}
	
	public void stop() {
		checkMedia();
		if(mediaInst != null) {
			mediaInst.dispose();
			mediaInst = null;
		}
	}
	
	private Media nextMedia() {
		switch(playPref) {
		case LOOP:
			int index = playerMedias.indexOf(this.mediaInst.media);
			return playerMedias.get((index + 1) % playerMedias.size());
		case RANDOM:
			return playerMedias.get(RandUtils.rangei(0, playerMedias.size()));
		case SINGLE:
			return null;
		case SINGLE_LOOP:
			return mediaInst.media;
		default:
			return null;
		}
	}
	
	public MediaInstance getPlayingMedia() {
		checkMedia();
		return mediaInst;
	}
	
	private void checkMedia() {
		if(mediaInst != null && playingSounds != null) {
			if(!playingSounds.containsKey(mediaInst))
				mediaInst = null;
		}
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if(!ClientUtils.isPlayerInGame() || event.phase == Phase.START)
			return;
		
		if(mediaInst != null && mediaInst.disposed) {
			Media next = nextMedia();
			if(next != null)
				startPlay(next);
		}
		
		if(mediaInst != null && mediaInst.isPaused) {
			sndSystem.pause(mediaInst.mediaUUID);
		}
	}
	
	@SubscribeEvent
	public void onDisconnect(ClientDisconnectionFromServerEvent event) {
		stop();
	}
	
}
