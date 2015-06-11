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

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import cn.annoreg.core.Registrant;
import cn.annoreg.core.RegistrationManager;
import cn.annoreg.core.RegistrationMod;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegMessageHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * Academy Craft Mod Main Class
 * @author acaly, WeathFolD, KS
 *
 */
@Mod(modid = "academy-craft", name = "AcademyCraft", version = AcademyCraft.VERSION)
@RegistrationMod(pkg = "cn.academy.", res = "academy", prefix = "ac_")
@Registrant
@RegEventHandler(Bus.Forge)
public class AcademyCraft {
    
	public static final boolean DEBUG_MODE = true;
	
	public static final String VERSION = "1.0a1";
	
    public static final String NET_CHANNEL = "academy-network";

    @Instance("academy-craft")
    public static AcademyCraft INSTANCE;

    public static Logger log = (Logger) LogManager.getLogger("AcademyCraft");
    public static Configuration config;

    @RegMessageHandler.WrapperInstance
    public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE
            .newSimpleChannel(AcademyCraft.NET_CHANNEL);
    
    @RegItem
    @RegItem.UTName("logo")
    public static Item logo;
    
    public static CreativeTabs cct = new CreativeTabs("AcademyCraft") {
        @Override
        public Item getTabIconItem() {
            return logo;
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        
        if(DEBUG_MODE)
        	((Logger)LogManager.getRootLogger()).setLevel(Level.DEBUG);
        
        log.info("Starting AcademyCraft");
        log.info("Copyright (c) Lambda Innovation, 2013-2015");
        log.info("http://ac.li-dev.cn/");
        
        RegistrationManager.INSTANCE.registerAll(this, "PreInit");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "Init");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "PostInit");
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "StartServer");
    }
    
    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
    	config.save();
    }
}