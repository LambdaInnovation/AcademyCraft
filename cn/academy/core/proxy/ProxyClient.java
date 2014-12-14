package cn.academy.core.proxy;

import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.core.AcademyCraftMod;

public class ProxyClient extends ProxyCommon {

	public void preInit() {
		EventHandlerClient.init();
		EventHandlerServer.init();
	}
	
	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading client proxy of Academy Craft.");
	}
	
}
