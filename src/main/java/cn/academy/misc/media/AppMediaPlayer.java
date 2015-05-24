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

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import paulscode.sound.SoundSystem;
import cn.annoreg.core.Registrant;
import cn.liutils.util3.RegUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * No gui yet, pre-programming
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
public class AppMediaPlayer {
	
	static final String[] builtInMedias = { "only_my_railgun", "sisters_noise", "level5_judgelight", "grow_slowly" };
	
	SoundManager soundManager;
	BiMap<ISound, String> playingSounds;
	SoundSystem sndSystem;
	
	MediaInstance media;
	
	public AppMediaPlayer() {}
	
	public void startPlay(String name) {
		stop();
		
		soundManager = RegUtils.getFieldInstance(SoundHandler.class, Minecraft.getMinecraft().getSoundHandler(), "sndManager", "field_147694_f");
		playingSounds = ((HashBiMap<String, ISound>) RegUtils.getFieldInstance(SoundManager.class, soundManager, "playingSounds", "field_148629_h")).inverse();
		sndSystem = RegUtils.getFieldInstance(SoundManager.class, soundManager, "sndSystem", "field_148620_e");
		
		media = new MediaInstance(name);
		soundManager.sndHandler.playSound(media);
		media.mediaUUID = playingSounds.get(media);
	}
	
	public boolean isPlaying() {
		return media != null && !media.disposed;
	}
	
	public boolean isPaused() {
		return media == null ? false : media.isPaused;
	}
	
	public String getMediaTitle() {
		return media == null ? null : media.getDisplayName();
	}
	
	public float getPlayedTime() {
		return media.getPlayTime();
	}
	
	public void pause() {
		if(media != null) {
			sndSystem.pause(media.mediaUUID);
			media.isPaused = true;
		}
	}
	
	public void resume() {
		if(media != null) {
			sndSystem.play(media.mediaUUID);
			media.isPaused = false;
		}
	}
	
	public void stop() {
		if(media != null) {
			media.dispose();
			media = null;
		}
	}
	
}
