/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.player;

import org.lwjgl.input.Keyboard;

import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cpw.mods.fml.common.FMLCommonHandler;

public class PlayerControlClient implements IKeyHandler {

	private static PlayerControlClient INSTANCE = null;
	
	public static void init() {
		if (INSTANCE == null)
			INSTANCE = new PlayerControlClient();
		FMLCommonHandler.instance().bus().register(INSTANCE);
		LIKeyProcess.instance.addKey("lock", Keyboard.KEY_P, false, INSTANCE);
	}
	
	@Override
	public void onKeyDown(int keyCode, boolean tickEnd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onKeyUp(int keyCode, boolean tickEnd) {
		if (tickEnd)
			return;
		if (!Keyboard.isKeyDown(Keyboard.KEY_O))
			return;
		if (keyCode == Keyboard.KEY_P) {
		}
		
	}

	@Override
	public void onKeyTick(int keyCode, boolean tickEnd) {
		// TODO Auto-generated method stub
		
	}
}
