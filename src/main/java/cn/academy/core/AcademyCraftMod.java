package cn.academy.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.academy.core.register.ACItems;
import cn.annoreg.core.RegistrationManager;
import cn.annoreg.core.RegistrationMod;
import cn.annoreg.mc.RegMessageHandler;
import cn.liutils.api.register.LIGuiHandler;
import cn.liutils.core.LIUtils;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;


/**
 * Academy Craft Mod 主类
 * @author acaly
 *
 */
@Mod(modid = "academy-craft", name = "AcademyCraft", version = AcademyCraftMod.VERSION)
@RegistrationMod(pkg = "cn.academy.", res = "academy", prefix = "ac_")
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

	/**
	 * 日志
	 */
	public static Logger log = LogManager.getLogger("AcademyCraft");
	
	public static Configuration config;
	
	/**
	 * 网络发包处理实例
	 */
	@RegMessageHandler.WrapperInstance
	public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel(AcademyCraftMod.NET_CHANNEL);
	
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
		
        RegistrationManager.INSTANCE.registerAll(this, LIUtils.REGISTER_TYPE_CONFIGURABLE);
		RegistrationManager.INSTANCE.registerAll(AcademyCraftMod.INSTANCE, LIUtils.REGISTER_TYPE_AUXGUI);
		RegistrationManager.INSTANCE.registerAll(AcademyCraftMod.INSTANCE, LIUtils.REGISTER_TYPE_KEYHANDLER);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "Block");
        RegistrationManager.INSTANCE.registerAll(this, "Item");
        RegistrationManager.INSTANCE.registerAll(this, "TileEntity");
        RegistrationManager.INSTANCE.registerAll(this, "Entity");
        RegistrationManager.INSTANCE.registerAll(this, "SubmoduleInit");
        RegistrationManager.INSTANCE.registerAll(this, "EventHandler");
        RegistrationManager.INSTANCE.registerAll(this, "MessageHandler");
        RegistrationManager.INSTANCE.registerAll(this, "GuiHandler");
        
        RegistrationManager.INSTANCE.registerAll(this, LIUtils.REGISTER_TYPE_AUXGUI);
        
        RegistrationManager.INSTANCE.registerAll(this, "Ability");
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}
	
	@EventHandler()
	public void serverStarting(FMLServerStartingEvent event) {
		RegistrationManager.INSTANCE.registerAll(this, "Command");
	}
	
	private static int nextNetID = 0;
	/**
	 * 获取下一个空闲的网络channelID。
	 */
	public static int getNextChannelID() {
		return nextNetID++;
	}
}
