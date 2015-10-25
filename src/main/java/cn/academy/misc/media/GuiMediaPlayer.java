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

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiScreen;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.ElementList;
import cn.liutils.cgui.gui.component.ProgressBar;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.VerticalDragBar;
import cn.liutils.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.GuiEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;

/**
 * @author WeAthFolD
 */
@Registrant
public class GuiMediaPlayer extends LIGuiScreen {
	
	static final ResourceLocation 
		T_PLAY = Resources.getTexture("guis/apps/media_player/play"),
		T_PAUSE = Resources.getTexture("guis/apps/media_player/pause");
	
	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/media_player.xml"));	
	}
	
	Widget pageMain;
	
	final MediaData data;
	final MediaPlayer player;
	
	public GuiMediaPlayer() {
		data = MediaData.get(Minecraft.getMinecraft().thePlayer);
		player = MediaPlayer.instance;
		
		init();
	}
	
	private void init() {
		pageMain = loaded.getWidget("back").copy();
		
		List<Media> installedMedias = data.getInstalledMediaList();
		
		player.updatePlayerMedias(installedMedias);
		
		{
			Widget area = pageMain.getWidget("area");
			ElementList list = new ElementList();
			
			for(Media m : installedMedias) {
				list.addWidget(createMedia(m));
			}
			
			area.addComponent(list);
		}
		
		EventLoader.load(pageMain, this);
		gui.addWidget(pageMain);
		
		gui.postEvent(new UpdateMediaEvent());
	}
	
	
	private Widget createMedia(Media media) {
		Widget ret = loaded.getWidget("t_one").copy();
		DrawTexture.get(ret.getWidget("icon")).texture = media.cover;
		TextBox.get(ret.getWidget("title")).content = media.getDisplayName();
		TextBox.get(ret.getWidget("desc")).content = media.getDesc();
		TextBox.get(ret.getWidget("time")).content = media.getLengthStr();
		
		ret.regEventHandler(MouseDownEvent.class, (Widget w, MouseDownEvent event) -> {
			if(w.isFocused()) {
				player.startPlay(media);
				gui.postEvent(new UpdateMediaEvent());
			}
		});
		
		return ret;
	}
	
    @Override
	public boolean doesGuiPauseGame() {
        return false;
    }
	
	@GuiCallback("scroll_bar")
	public void onProgressChange(Widget w, DraggedEvent event) {
		VerticalDragBar vdb = VerticalDragBar.get(w);
		
		ElementList list = ElementList.get(pageMain.getWidget("area"));
		list.setProgress((int) (vdb.getProgress() * list.getMaxProgress()));
	}
	
	@GuiCallback("pop")
	public void onPopDown(Widget w, MouseDownEvent event) {
		if(player.isPlaying()) {
			if(player.isPaused())
				player.resume();
			else
				player.pause();
		} else {
			player.startPlay();
		}
		
		updatePopState();
		gui.postEvent(new UpdateMediaEvent());
	}
	
	@GuiCallback("stop")
	public void onStop(Widget w, MouseDownEvent event) {
		player.stop();
		gui.postEvent(new UpdateMediaEvent());
	}
	
	@GuiCallback("progress")
	public void updateProgress(Widget w, FrameEvent event) {
		MediaInstance mi = player.getPlayingMedia();
		ProgressBar.get(w).progress = mi == null ? 0.0 : (double)mi.getPlayTime() / mi.media.length;
	}
	
	@GuiCallback("play_time")
	public void updateTime(Widget w, FrameEvent event) {
		MediaInstance mi = player.getPlayingMedia();
		TextBox.get(w).content = mi == null ? "" : Media.getPlayingTime(mi.getPlayTime());
	}
	
	@GuiCallback("title")
	public void updateTitle(Widget w, UpdateMediaEvent event) {
		MediaInstance mi = player.getPlayingMedia();
		
		TextBox.get(w).content = mi == null ? "" : mi.media.getDisplayName();
	}
	
	@GuiCallback
	public void ups(Widget w, UpdateMediaEvent event) {
		updatePopState();
	}
	
	private void updatePopState() {
		DrawTexture.get(pageMain.getWidget("pop")).texture = (player.isPlaying() && !player.isPaused()) ? T_PAUSE : T_PLAY;
	}
	
	private class UpdateMediaEvent implements GuiEvent {} 

}
