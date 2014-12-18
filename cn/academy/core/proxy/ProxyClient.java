package cn.academy.core.proxy;

import net.minecraftforge.client.MinecraftForgeClient;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.core.AcademyCraftMod;

public class ProxyClient extends ProxyCommon {

	@Override
	public void preInit() {
	}
	
	@Override
	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading client proxy of Academy Craft.");
		EventHandlerClient.init();
		EventHandlerServer.init();
		PresetManager.init();
	}
	
}
