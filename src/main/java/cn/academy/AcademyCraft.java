package cn.academy;

import cn.academy.analytic.AnalyticDataListener;
import cn.lambdalib2.crafting.CustomMappingHelper;
import cn.lambdalib2.crafting.RecipeRegistry;
import cn.lambdalib2.registry.RegistryMod;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Debug;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map.Entry;

/**
 * Academy Craft Mod Main Class
 * 
 * @author acaly, WeathFolD, KS
 *
 */
@Mod(modid = "academy", name = "AcademyCraft", version = AcademyCraft.VERSION,
     dependencies = "required-after:lambdalib2@@LAMBDA_LIB_VERSION@")
@RegistryMod(rootPackage = "cn.academy.", resourceDomain = "academy")
public class AcademyCraft {

    @Instance("academy-craft")
    public static AcademyCraft INSTANCE;

    public static final String VERSION = "@VERSION@";

    public static final boolean DEBUG_MODE = VERSION.startsWith("@");

    public static final Logger log = LogManager.getLogger("AcademyCraft");

    static final String[] scripts = { "generic", "ability", "electromaster", "teleporter", "meltdowner",
            "generic_skills" };

    public static Configuration config;

    public static RecipeRegistry recipes;

    public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel("academy-network");

    public static AnalyticDataListener analyticDataListener;

    public static CreativeTabs cct = new CreativeTabs("AcademyCraft") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ACItems.logo);
        }
    };

    @StateEventCallback(priority = 1)
    private static void preInit(FMLPreInitializationEvent event) {
        log.info("Starting AcademyCraft");
        log.info("Copyright (c) Lambda Innovation, 2013-2018");
        log.info("https://ac.li-dev.cn/");
        recipes = new RecipeRegistry();

        config = new Configuration(event.getSuggestedConfigurationFile());
        Boolean analyticFlag = config.getBoolean("analysis","generic",true,"switch for analytic system");
        if(analyticFlag){
            analyticDataListener = AnalyticDataListener.instance;
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        OreDictionary.registerOre("plateIron", ACItems.reinforced_iron_plate);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        // Load script, where names now are available
        recipes.addRecipeFromResourceLocation(new ResourceLocation("academy:recipes/default.recipe"));

        if (DEBUG_MODE) {
            Debug.log("|-------------------------------------------------------");
            Debug.log("| AC Recipe Name Mappings");
            Debug.log("|--------------------------|----------------------------");
            Debug.log(String.format("| %-25s| Object Name", "Recipe Name"));
            Debug.log("|--------------------------|----------------------------");
            for (Entry<String, Object> entry : recipes.getNameMappingForDebug().entrySet()) {
                Object obj = entry.getValue();
                String str1 = entry.getKey(), str2;
                if (obj instanceof Item) {
                    str2 = I18n.translateToLocal(((Item) obj).getTranslationKey() + ".name");
                } else if (obj instanceof Block) {
                    str2 = I18n.translateToLocal(((Block) obj).getTranslationKey() + ".name");
                } else {
                    str2 = obj.toString();
                }
                Debug.log(String.format("| %-25s| %s", str1, str2));
            }
            Debug.log("|-------------------------------------------------------");
        }

        recipes = null; // Release and have fun GC
        config.save();
    }
    
    @SideOnly(Side.CLIENT)
    @EventHandler
    public void postInit2(FMLPostInitializationEvent event) {
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ACConfig.updateConfig(null);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        config.save();
    }

    @SubscribeEvent
    public void onClientDisconnectionFromServer(
        ClientDisconnectionFromServerEvent e)
    {
        config.save();
    }

    public static void addToRecipe(Class klass) {
        CustomMappingHelper.addMapping(recipes, klass);
    }
    /**
     * Simply a fast route to print debug message.
     */
    public static void debug(Object msg) {
        if (DEBUG_MODE) {
            log.info(msg);
        }
    }

}