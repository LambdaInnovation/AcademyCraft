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
