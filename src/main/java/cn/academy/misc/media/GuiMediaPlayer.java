/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.CGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.*;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.GuiEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * @author WeAthFolD
 */
@Registrant
public class GuiMediaPlayer extends CGuiScreen {
    
    static final ResourceLocation 
        T_PLAY = Resources.getTexture("guis/apps/media_player/play"),
        T_PAUSE = Resources.getTexture("guis/apps/media_player/pause");
    
    static WidgetContainer document = CGUIDocument.panicRead(new ResourceLocation("academy:guis/media_player.xml"));
    
    Widget pageMain;
    
    final MediaData data;
    final MediaPlayer player;
    
    public GuiMediaPlayer() {
        data = MediaData.get(Minecraft.getMinecraft().thePlayer);
        player = MediaPlayer.instance;
        
        init();
    }
    
    private void init() {
        pageMain = document.getWidget("back").copy();
        
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

        pageMain.getWidget("scroll_bar").listen(DraggedEvent.class, (w, e) -> {
            VerticalDragBar vdb = VerticalDragBar.get(w);

            ElementList list = ElementList.get(pageMain.getWidget("area"));
            list.setProgress((int) (vdb.getProgress() * list.getMaxProgress()));
        });

        pageMain.getWidget("pop").listen(LeftClickEvent.class, (w, e) -> {
            if(player.isPlaying()) {
                if(player.isPaused())
                    player.resume();
                else
                    player.pause();
            } else {
                player.startPlay();
            }

            updatePopState();
            gui.postEventHierarchically(new UpdateMediaEvent());
        });

        pageMain.getWidget("stop").listen(LeftClickEvent.class, (w, e) -> {
            player.stop();
            gui.postEventHierarchically(new UpdateMediaEvent());
        });

        pageMain.getWidget("progress").listen(FrameEvent.class, (w, e) -> {
            MediaInstance mi = player.getPlayingMedia();
            ProgressBar.get(w).progress = mi == null ? 0.0 : (double)mi.getPlayTime() / mi.media.length;
        });

        pageMain.getWidget("play_time").listen(FrameEvent.class, (w, e) -> {
            MediaInstance mi = player.getPlayingMedia();
            TextBox.get(w).content = mi == null ? "" : Media.getPlayingTime(mi.getPlayTime());
        });

        pageMain.getWidget("title").listen(UpdateMediaEvent.class, (w, e) -> {
            MediaInstance mi = player.getPlayingMedia();
            TextBox.get(w).content = mi == null ? "" : mi.media.getDisplayName();
        });

        pageMain.listen(UpdateMediaEvent.class, (w, e) -> updatePopState());

        gui.addWidget(pageMain);
        
        gui.postEventHierarchically(new UpdateMediaEvent());
    }
    
    
    private Widget createMedia(Media media) {
        Widget ret = document.getWidget("t_one").copy();
        DrawTexture.get(ret.getWidget("icon")).texture = media.cover;
        TextBox.get(ret.getWidget("title")).content = media.getDisplayName();
        TextBox.get(ret.getWidget("desc")).content = media.getDesc();
        TextBox.get(ret.getWidget("time")).content = media.getLengthStr();
        
        ret.listen(LeftClickEvent.class, (w, e) -> {
            if(w.isFocused()) {
                player.startPlay(media);
                gui.postEventHierarchically(new UpdateMediaEvent());
            }
        });
        
        return ret;
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    private void updatePopState() {
        DrawTexture.get(pageMain.getWidget("pop")).texture = (player.isPlaying() && !player.isPaused()) ? T_PAUSE : T_PLAY;
    }
    
    private class UpdateMediaEvent implements GuiEvent {} 

}
