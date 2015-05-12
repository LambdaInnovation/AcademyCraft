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
package cn.academy.test.mp3;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import cn.academy.misc.media.AppMediaPlayer;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.registry.AttachKeyHandlerRegistry.RegAttachKeyHandler;
import cn.liutils.util.ClientUtils;

/**
 * @author WeAthFolD
 *
 */
@RegistrationClass
public class MP3PlayerTest {

	@RegAttachKeyHandler(clazz = KeyHandler.class)
	public static int KEYID = Keyboard.KEY_O;
	
	public static class KeyHandler implements IKeyHandler {
		
		AppMediaPlayer mediaPlayer = new AppMediaPlayer();

		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(!tickEnd && ClientUtils.isPlayerInGame()) {
				if(mediaPlayer.isPlaying()) {
					mediaPlayer.resume();
				} else {
					mediaPlayer.startPlay("only_my_railgun");
				}
			}
		}

		@Override
		public void onKeyUp(int keyCode, boolean tickEnd) {
			if(!tickEnd) {
				mediaPlayer.pause();
			}
		}

		@Override
		public void onKeyTick(int keyCode, boolean tickEnd) {
			if(!tickEnd) {
				EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			}
		}
		
	}
	
	
	
}
