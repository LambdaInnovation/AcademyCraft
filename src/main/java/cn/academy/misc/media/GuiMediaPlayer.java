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
import cn.lambdalib.cgui.gui.CGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.*;
import cn.lambdalib.cgui.gui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.gui.event.*;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Color;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

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

    final MediaPlayer player;
    
    public GuiMediaPlayer() {
        player = MediaPlayer.instance;
        init();
    }
    
    private void init() {
        pageMain = document.getWidget("back").copy();
        
        {
            Widget area = pageMain.getWidget("area");
            ElementList list = new ElementList();
            
            for(ACMedia m : MediaManager.medias()) {
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
            PlayState state = player.getState();
            if (state == PlayState.STOPPED) {
                ACMedia firstMedia = MediaManager.get(0);
                player.startPlay(firstMedia);
            } else {
                if (state == PlayState.PAUSED) {
                    player.resume();
                } else {
                    player.pause();
                }
            }

            gui.postEventHierarchically(new UpdateMediaEvent());
        });

        pageMain.getWidget("stop").listen(LeftClickEvent.class, (w, e) -> {
            player.stop();
            gui.postEventHierarchically(new UpdateMediaEvent());
        });

        pageMain.getWidget("progress").listen(FrameEvent.class, (w, e) -> {
            ACMedia mi = player.currentMedia;
            ProgressBar.get(w).progress = mi == null ? 0.0 : (double) MediaRuntime.getPlayedTime(mi) / mi.getLength();
        });

        pageMain.getWidget("play_time").listen(FrameEvent.class, (w, e) -> {
            ACMedia mi = player.currentMedia;
            TextBox.get(w).content = mi == null ? "00:00" : MediaRuntime.getDisplayTime((int) MediaRuntime.getPlayedTime(mi));
        });

        pageMain.getWidget("title").listen(UpdateMediaEvent.class, (w, e) -> {
            ACMedia mi = player.currentMedia;
            TextBox.get(w).content = mi == null ? "" : mi.getName();
        });

        pageMain.getWidget("volume_bar").getComponent(DragBar.class).setProgress(MediaRuntime.getVolume());

        pageMain.getWidget("volume_bar").listen(DragBar.DraggedEvent.class, (w, e) -> {
            float volume = MathUtils.clampf(0, 1, (float) DragBar.get(w).getProgress());
            MediaRuntime.setVolume(volume);
            ACMedia current = player.getCurrentMedia();
            if (current != null) {
                MediaRuntime.setMediaVolume(current, volume);
            }
        });

        pageMain.listen(UpdateMediaEvent.class, (w, e) -> updatePopState());

        gui.addWidget(pageMain);
        
        gui.postEventHierarchically(new UpdateMediaEvent());
    }
    
    
    private Widget createMedia(ACMedia media) {
        Widget ret = document.getWidget("back/t_one").copy();
        ret.transform.doesDraw = true;

        DrawTexture.get(ret.getWidget("icon")).texture = media.getCover();
        TextBox.get(ret.getWidget("title")).content = media.getName();
        TextBox.get(ret.getWidget("desc")).content = media.getDesc();
        TextBox.get(ret.getWidget("time")).content = MediaRuntime.getDisplayTime((int) media.getLength());

        if (media.isExternal()) {
            wrapEdit(ret.getWidget("btn_edit_name"), ret.getWidget("title"), newName -> {
                ACMedia.updateExternalName(media, newName);
            });
            wrapEdit(ret.getWidget("btn_edit_desc"), ret.getWidget("desc"), newDesc -> {
                ACMedia.updateExternalDesc(media, newDesc);
            });
        }
        
        ret.listen(LeftClickEvent.class, (w, e) -> {
            if(w.isFocused()) {
                player.startPlay(media);
                gui.postEventHierarchically(new UpdateMediaEvent());
            }
        });
        
        return ret;
    }

    private void wrapEdit(Widget button, Widget box, Consumer<String> callback) {
        button.transform.doesDraw = true;

        DrawTexture dt = new DrawTexture(null).setColor(Color.monoBlend(.4, 0));
        TextBox textBox = box.getComponent(TextBox.class);
        box.addComponent(dt);

        boolean state[] = new boolean[1];
        button.listen(LeftClickEvent.class, (w, e) -> {
            if (!state[0]) {
                state[0] = true;
                dt.color.a = .2;
                textBox.allowEdit = true;
                box.gainFocus();
                box.transform.doesListenKey = true;
            }
        });

        IGuiEventHandler handler = (w, e) -> {
            if (state[0]) {
                state[0] = false;
                textBox.allowEdit = false;
                box.transform.doesListenKey = false;
                dt.color.a = 0;
                callback.accept(textBox.content);
            }
        };

        box.listen(ConfirmInputEvent.class, handler);
        box.listen(LostFocusEvent.class, handler);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    private void updatePopState() {
        PlayState state = player.getState();
        DrawTexture.get(pageMain.getWidget("pop")).texture = (state == PlayState.PAUSED) ? T_PAUSE : T_PLAY;
    }
    
    private class UpdateMediaEvent implements GuiEvent {} 

}
