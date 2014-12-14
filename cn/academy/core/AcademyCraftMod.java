package cn.academy.core;

import org.apache.logging.log4j.Logger;

import cn.academy.core.proxy.ProxyCommon;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.SidedProxy;


/**
 * Academy Craft Mod 主类
 * @author acaly
 *
 */

@Mod(modid = "academy-craft", name = "AcademyCraft", version = AcademyCraftMod.VERSION)
public class AcademyCraftMod {

	/**
	 * 当前版本
	 */
	public static final String VERSION = "0.0.1dev";

	/**
	 * 主类实例
	 */
	@Instance("academy-craft")
	public static AcademyCraftMod INSTANCE;
	

	@SidedProxy(clientSide = "cn.academy.core.proxy.ProxyClient", serverSide = "cn.academy.core.proxy.ProxyCommon")
	/**
	 * 加载代理
	 */
	public static ProxyCommon proxy;

	/**
	 * 日志
	 */
	public static Logger log = FMLLog.getLogger();
	
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit();
	}
}
