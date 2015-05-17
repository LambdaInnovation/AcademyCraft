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
package cn.academy.ability.api.preset;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.client.ui.KeyHint;
import cn.liutils.api.key.LIKeyProcess;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public final class Preset {
	
	
	@SideOnly(Side.CLIENT)
	public List<KeyHint> toKeyHints() {
		List<KeyHint> list = new ArrayList();
		
		list.add(new KeyHint("test/railgun.png", Keyboard.KEY_R, "Railgun"));
		list.add(new KeyHint("test/mineview.png", Keyboard.KEY_F, "Mine Scope"));
		list.add(new KeyHint("test/mineview.png", LIKeyProcess.MOUSE_LEFT, "Mine Scope"));
		
		return list;
	}
	
}
