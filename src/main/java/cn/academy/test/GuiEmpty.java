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
package cn.academy.test;

import org.lwjgl.input.Keyboard;

import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.liutils.util.helper.KeyHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
public class GuiEmpty extends GuiScreen {
	
	@RegACKeyHandler(name = "wtf", defaultKey = Keyboard.KEY_K, dynamic = true)
	public static KeyHandler kh = new KeyHandler() {
		@Override
		public void onKeyDown() {
			Minecraft.getMinecraft().displayGuiScreen(new GuiEmpty());
		}
	};
	
	@Override
    public boolean doesGuiPauseGame() {
        return false;
    }
	
}
