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
package cn.academy.terminal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

import cn.academy.core.client.Resources;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.helper.KeyHandler;

/**
 * @author WeAthFolD
 */
@Registrant
public class TerminalUI extends AuxGui {

	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/data_terminal.xml"));
	}
	
	boolean isActivated = false;
	TerminalMouseHelper helper;
	MouseHelper oldHelper;
	
	public TerminalUI() {
		//TODO
	}
	
	@RegACKeyHandler(name = "open_data_terminal", defaultKey = Keyboard.KEY_U)
	public static KeyHandler keyHandler = new KeyHandler() {
		TerminalUI current;
		
		public void onKeyDown() {
			if(current != null) {
				current.dispose();
			}
			current = new TerminalUI();
			register(current);
		}
	};
	
	@Override
	public void onAdded() {
		Minecraft mc = Minecraft.getMinecraft();
		oldHelper = mc.mouseHelper;
		mc.mouseHelper = helper = new TerminalMouseHelper();
	}
	
	@Override
	public void onDisposed() {
		Minecraft mc = Minecraft.getMinecraft();
		mc.mouseHelper = oldHelper;
	}

	@Override
	public boolean isForeground() {
		return true;
	}

	@Override
	public void draw(ScaledResolution sr) {
		
	}

	private static ResourceLocation tex(String name) {
		return Resources.getTexture("guis/data_terminal/" + name);
	}
	
}
