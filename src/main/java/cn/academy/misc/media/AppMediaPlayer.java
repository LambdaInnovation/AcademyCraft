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

import net.minecraft.nbt.NBTTagCompound;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.registry.AppRegistration.RegApp;
import cn.annoreg.core.Registrant;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class AppMediaPlayer extends App {
	
	@RegApp
	public static AppMediaPlayer instance = new AppMediaPlayer();

	AppMediaPlayer() {
		super("media_player");
		setPreInstalled();
	}

	@Override
	public AppEnvironment createEnvironment() {
		return new AppEnvironment() {
			@Override
			public void onStart() {
				GuiMediaPlayer.guiHandler.openClientGui();
			}
		};
	}

}
