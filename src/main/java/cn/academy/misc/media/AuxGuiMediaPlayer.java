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

import net.minecraft.util.ResourceLocation;
import cn.academy.core.client.ui.ACHud;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegInit
public class AuxGuiMediaPlayer {
	
	public static void init() {
		Widget w = CGUIDocLoader.load(new ResourceLocation("academy:guis/media_player_aux.xml")).getWidget("base");

		w.getWidget("progress").listen(FrameEvent.class, (__, e) -> {
			MediaInstance inst = MediaPlayer.instance.getPlayingMedia();
			ProgressBar.get(w).progress = (double) inst.getPlayTime() / inst.media.length;
		});

		w.getWidget("title").listen(FrameEvent.class, (__, e) -> {
			MediaInstance inst = MediaPlayer.instance.getPlayingMedia();
			TextBox.get(w).content = inst.media.getDisplayName();
		});

		w.getWidget("time").listen(FrameEvent.class, (__, e) -> {
			MediaInstance inst = MediaPlayer.instance.getPlayingMedia();
			TextBox.get(w).content = Media.getPlayingTime(inst.getPlayTime());
		});

		ACHud.instance.addElement(w, () -> MediaPlayer.instance.isPlaying());
	}

}
