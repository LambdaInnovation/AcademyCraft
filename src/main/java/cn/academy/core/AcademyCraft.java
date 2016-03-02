/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.core.RegistrationManager;
import cn.lambdalib.annoreg.core.RegistrationMod;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegMessageHandler;
import cn.lambdalib.crafting.CustomMappingHelper;
import cn.lambdalib.crafting.RecipeRegistry;
import cn.lambdalib.ripple.ScriptFunction;
import cn.lambdalib.ripple.ScriptProgram;
import cn.lambdalib.util.client.shader.ShaderProgram;
import cn.lambdalib.util.reschk.ResourceCheck;
import cn.lambdalib.util.version.VersionUpdateUrl;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map.Entry;

/**
 * Academy Craft Mod Main Class
 * 
 * @author acaly, WeathFolD, KS
 *
 */
@Mod(modid = "academy-craft", name = "AcademyCraft", version = AcademyCraft.VERSION,
     dependencies = "required-after:LambdaLib@[1.1.1]") // LambdaLib is currently unstable. Supports only one version.
@RegistrationMod(pkg = "cn.academy.", res = "academy", prefix = "ac_")
@Registrant
@VersionUpdateUrl(repoUrl="github.com/LambdaInnovation/AcademyCraft")
public class AcademyCraft {

    @Instance("academy-craft")
    public static AcademyCraft INSTANCE;

    public static final boolean DEBUG_MODE = false;

    public static final String VERSION = "1.0pr3_1";

    public static final Logger log = LogManager.getLogger("AcademyCraft");

    static final String[] scripts = { "generic", "ability", "electromaster", "teleporter", "meltdowner",
            "generic_skills" };

    public static Configuration config;

    public static RecipeRegistry recipes = new RecipeRegistry();

    @RegMessageHandler.WrapperInstance
    public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel("academy-network");

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
        log.info("Starting AcademyCraft");
        log.info("Copyright (c) Lambda Innovation, 2013-2015");
        log.info("http://ac.li-dev.cn/");

        config = new Configuration(event.getSuggestedConfigurationFile());

        RegistrationManager.INSTANCE.registerAll(this, "PreInit");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "Init");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Finally we are hit by annoreg's loading order problem ..
        // Below is a kinda hacking solution to manullay override loading order, but clearly a better
        //  mechanism is needed

        // Load recipes names before loading script
        RegistrationManager.INSTANCE.registerAll(this, "AC_RecipeNames");

        // Load script, where names now are available
        recipes.addRecipeFromResourceLocation(new ResourceLocation("academy:recipes/default.recipe"));

        // PostInit stage, including tutorial init, depends on registered recipes
        RegistrationManager.INSTANCE.registerAll(this, "PostInit");

        if (DEBUG_MODE) {
            System.out.printf("|-------------------------------------------------------\n");
            System.out.printf("| AC Recipe Name Mappings\n");
            System.out.printf("|--------------------------|----------------------------\n");
            System.out.printf(String.format("| %-25s| Object Name\n", "Recipe Name"));
            System.out.printf("|--------------------------|----------------------------\n");
            for (Entry<String, Object> entry : recipes.nameMapping.entrySet()) {
                Object obj = entry.getValue();
                String str1 = entry.getKey(), str2;
                if (obj instanceof Item) {
                    str2 = StatCollector.translateToLocal(((Item) obj).getUnlocalizedName() + ".name");
                } else if (obj instanceof Block) {
                    str2 = StatCollector.translateToLocal(((Block) obj).getUnlocalizedName() + ".name");
                } else {
                    str2 = obj.toString();
                }
                System.out.printf(String.format("| %-25s| %s\n", str1, str2));
            }
            System.out.printf("|-------------------------------------------------------\n");
        }

        recipes = null; // Release and have fun GC
        config.save();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        RegistrationManager.INSTANCE.registerAll(this, "StartServer");
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
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