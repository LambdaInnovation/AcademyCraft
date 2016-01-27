/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.client.ui.ACHud;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@SideOnly(Side.CLIENT)
public class AuxGuiMediaPlayer {

    @RegInitCallback
    public static void init() {

        Widget base = CGUIDocument.panicRead(new ResourceLocation("academy:guis/media_player_aux.xml")).getWidget("base");

        ACHud.instance.addElement(base, () -> MediaPlayer.instance.isPlaying(), "media", base.copy());

        base.getWidget("progress").listen(FrameEvent.class, (w, e) -> {
            MediaInstance inst = MediaPlayer.instance.getPlayingMedia();
            ProgressBar.get(w).progress = (double) inst.getPlayTime() / inst.media.length;
        });

        base.getWidget("title").listen(FrameEvent.class, (w, e) -> {
            MediaInstance inst = MediaPlayer.instance.getPlayingMedia();
            TextBox.get(w).content = inst.media.getDisplayName();
        });

        base.getWidget("time").listen(FrameEvent.class, (w, e) -> {
            MediaInstance inst = MediaPlayer.instance.getPlayingMedia();
            TextBox.get(w).content = Media.getPlayingTime(inst.getPlayTime());
        });
    }

}
