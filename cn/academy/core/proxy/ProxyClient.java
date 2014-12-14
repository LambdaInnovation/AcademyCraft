package cn.academy.core.proxy;

import cn.academy.core.AcademyCraftMod;

public class ProxyClient extends ProxyCommon {

	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading client proxy of Academy Craft.");
	}
	
}
