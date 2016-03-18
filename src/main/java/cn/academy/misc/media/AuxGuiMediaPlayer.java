/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.client.ui.ACHud;
import cn.academy.misc.media.MediaRuntime.PlayState;
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

        ACHud.instance.addElement(base,
                () -> MediaPlayer.instance.getState() == PlayState.PLAYING,
                "media", base.copy());

        base.getWidget("progress").listen(FrameEvent.class, (w, e) -> {
            ACMedia inst = MediaPlayer.instance.currentMedia;
            ProgressBar.get(w).progress = (double) MediaRuntime.getPlayedTime(inst) / inst.getLength();
        });

        base.getWidget("title").listen(FrameEvent.class, (w, e) -> {
            ACMedia inst = MediaPlayer.instance.currentMedia;
            TextBox.get(w).content = inst.getName();
        });

        base.getWidget("time").listen(FrameEvent.class, (w, e) -> {
            ACMedia inst = MediaPlayer.instance.currentMedia;
            TextBox.get(w).content = MediaRuntime.getDisplayTime((int) (MediaRuntime.getPlayedTime(inst)));
        });
    }

}
