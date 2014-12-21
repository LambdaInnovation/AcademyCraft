package cn.academy.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ProxyCommon;
import cn.academy.core.register.ACBlocks;
import cn.academy.core.register.ACItems;
import cn.liutils.api.register.LIGuiHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;


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

	public static final String NET_CHANNEL = "academy-network";

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
	public static Logger log = LogManager.getLogger("AcademyCraft");
	
	public static Configuration config;
	
	/**
	 * 网络发包处理实例
	 */
	public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel(AcademyCraftMod.NET_CHANNEL);
	
	/**
	 * GUI处理器
	 */
	public static LIGuiHandler guiHandler = new LIGuiHandler();
	
	/**
	 * 创造栏
	 */
	public static CreativeTabs cct = new CreativeTabs("AcademyCraft") {
		@Override
		public Item getTabIconItem() {
			return ACItems.logo;
		}
	};
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		config = new Configuration(event.getSuggestedConfigurationFile());
		
		AbilityDataMain.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, guiHandler);
		
		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		
		ACItems.init(config);
		ACBlocks.init(config);
		
		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit();
	}
	
	private static int nextNetID = 0;
	/**
	 * 获取下一个空闲的网络channelID。
	 */
	public static int getNextChannelID() {
		return nextNetID++;
	}
}
