/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core;

import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.academy.core.command.CmdCheating;
import cn.academy.core.command.CmdDataSet;
import cn.academy.core.command.CmdDataView;
import cn.academy.core.proxy.ACCommonProps;
import cn.academy.core.register.ACItems;
import cn.academy.core.register.ACMiscReg;
import cn.academy.core.register.ExtendedDataRegistration;
import cn.annoreg.core.RegistrationManager;
import cn.annoreg.core.RegistrationMod;
import cn.annoreg.mc.RegMessageHandler;
import cn.liutils.core.LIUtils;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;


/**
 * Academy Craft Mod 主类
 * @author acaly, WeathFolD, KS
 *
 */
@Mod(modid = "academy-craft", name = "AcademyCraft", version = AcademyCraft.VERSION)
@RegistrationMod(pkg = "cn.academy.", res = "academy", prefix = "ac_")
public class AcademyCraft {

	/**
	 * 当前版本
	 */
	public static final String VERSION = "1.0beta1";

	public static final String NET_CHANNEL = "academy-network";

	/**
	 * 主类实例
	 */
	@Instance("academy-craft")
	public static AcademyCraft INSTANCE;

	/**
	 * 日志
	 */
	public static Logger log = LogManager.getLogger("AcademyCraft");
	
	public static Configuration config;
	
	/**
	 * 网络发包处理实例
	 */
	@RegMessageHandler.WrapperInstance
	public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel(AcademyCraft.NET_CHANNEL);
	
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
		RegistrationManager.INSTANCE.registerAll(this, "PreInit");
		
		log.info("Starting AcademyCraft " + VERSION);
		log.info("Copyright (c) Lambda Innovation, 2013-2015");
		log.info("http://ac.li-dev.cn/");
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "Init");
        
        ACMiscReg.regRecipe();
        ACMiscReg.regSmelting();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "PostInit");
	}
	
	@EventHandler()
	public void serverStarting(FMLServerStartingEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "StartServer");
        CommandHandler ch = (CommandHandler) MinecraftServer.getServer().getCommandManager();
        
        //Manually register for debug testing
        if(ACCommonProps.debugMode) {
        	ch.registerCommand(new CmdDataView());
        }
        
        ch.registerCommand(new CmdCheating());
	}
}
