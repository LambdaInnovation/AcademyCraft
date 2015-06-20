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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.ResourceLocation;
import paulscode.sound.SoundSystem;
import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.liutils.util.client.ClientUtils;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.RegistryUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * No gui yet, pre-programming
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
public class MediaPlayer {
	
	static final Map<String, Media> medias = new HashMap();
	static final List<Media> mediaList = new ArrayList();
	
	static {
		addMedia("only_my_railgun", 257);
	}
	
	public static void addMedia(String name, int len) {
		addMedia(new Media(name, len));
	}
	
	public static void addMedia(Media media) {
		mediaList.add(media);
		media.id = mediaList.size() - 1;
		
		medias.put(media.name, media);
	}
	
	public static Media getMedia(int id) {
		return mediaList.get(id);
	}
	
	public static Media getMedia(String name) {
		return medias.get(name);
	}
	
	public static Collection<Media> getMedias() {
		return medias.values();
	}
	
	public static int getMediaCount() {
		return mediaList.size();
	}
	
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
	
	public PlayPref playPref = PlayPref.LOOP;
	
	SoundManager soundManager;
	BiMap<ISound, String> playingSounds;
	SoundSystem sndSystem;
	
	MediaInstance mediaInst;
	
	MediaPlayer() {}
	
	public void startPlay(Media media) {
		stop();
		
		soundManager = RegistryUtils.getFieldInstance(SoundHandler.class, Minecraft.getMinecraft().getSoundHandler(), "sndManager", "field_147694_f");
		playingSounds = ((HashBiMap<String, ISound>) RegistryUtils.getFieldInstance(SoundManager.class, soundManager, "playingSounds", "field_148629_h")).inverse();
		sndSystem = RegistryUtils.getFieldInstance(SoundManager.class, soundManager, "sndSystem", "field_148620_e");
		
		mediaInst = new MediaInstance(media);
		soundManager.sndHandler.playSound(mediaInst);
		mediaInst.mediaUUID = playingSounds.get(mediaInst);
	}
	
	public void startPlay(String name) {
		startPlay(getMedia(name));
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
		if(mediaInst != null) {
			sndSystem.pause(mediaInst.mediaUUID);
			mediaInst.isPaused = true;
		}
	}
	
	public void resume() {
		if(mediaInst != null) {
			sndSystem.play(mediaInst.mediaUUID);
			mediaInst.isPaused = false;
		}
	}
	
	public void stop() {
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
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if(!ClientUtils.isPlayerInGame() || event.phase == Phase.START)
			return;
		
		if(mediaInst != null && mediaInst.disposed) {
			Media next = nextMedia();
			if(next != null)
				startPlay(next);
		}
	}
	
}
