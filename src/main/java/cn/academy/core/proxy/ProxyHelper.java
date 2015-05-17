package cn.academy.core.proxy;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Helper class used to get thread dependent proxy.
 * Note that this is a thread proxy, not a side proxy.
 * @author acaly
 *
 */
public final class ProxyHelper {
	
	private static ThreadLocal<ThreadProxy> proxy = new ThreadLocal<ThreadProxy>() {

        @Override protected ThreadProxy initialValue() {
        	try {
        		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
					return (ThreadProxy) Class.forName("cn.academy.core.proxy.ClientThreadProxy")
							.newInstance();
        		} else {
					return (ThreadProxy) Class.forName("cn.academy.core.proxy.ServerThreadProxy")
							.newInstance();
        		}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
        }
        
	};
	
	public static ThreadProxy get() {
		return proxy.get();
	}
	
}
