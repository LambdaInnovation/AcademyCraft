package cn.academy.api.player;

import org.lwjgl.input.Keyboard;

import cn.liutils.api.client.key.IKeyHandler;
import cn.liutils.api.util.GenericUtils;
import cn.liutils.core.client.register.LIKeyProcess;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

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
