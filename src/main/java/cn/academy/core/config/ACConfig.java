package cn.academy.core.config;

import cn.academy.core.AcademyCraft;
import cn.academy.core.network.NetworkManager;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.generic.RegistryUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;

@Registrant
public final class ACConfig {

    private ACConfig() {}

    private static Config config;

    private static String lastError = null;

    private static void __init() {
        Logger log = AcademyCraft.log;

        ResourceLocation defaultRes = new ResourceLocation("academy:config/default.conf");

        Reader reader = new InputStreamReader(RegistryUtils.getResourceStream(defaultRes));

        config = ConfigFactory.parseReader(reader);

        File customFile = new File("config/academy-craft-data.conf");
        if (!customFile.isFile()) {
            try {
                Files.copy(RegistryUtils.getResourceStream(defaultRes), customFile.toPath());
            } catch (IOException ex) {
                log.error("Error when copying config template to config folder", ex);
            }
        }

        try {
            Config customConfig = ConfigFactory.parseFile(customFile);

            config = customConfig.withFallback(config);
        } catch (RuntimeException ex) {
            log.error("An error occured parsing custom config", ex);
            lastError = ex.toString();
        }
    }

    public static void updateConfig(Config cfg)
    {
        if(cfg==null)
            __init();
        else
            config=cfg;
    }


    @RegInitCallback
    private static void __forceLoadAtInit() {
        instance();

        FMLCommonHandler.instance().bus().register(new LoginEvents());
    }

    public static Config instance() {
        synchronized (ACConfig.class) {
            if (config == null) {
                __init();
            }

            return config;
        }
    }

    /**
     * @return Last error, will clear the error storage. null if no error.
     */
    public static String fetchLastError() {
        String ret = lastError;
        lastError = null;
        return ret;
    }

    public static class LoginEvents {
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent evt) {
            String err = fetchLastError();

            if (err != null) {
                evt.player.addChatMessage(new ChatComponentTranslation("ac.data_config_parse_fail"));
                evt.player.addChatMessage(new ChatComponentTranslation(err));
            }
            if(!evt.player.worldObj.isRemote)
                NetworkManager.sendTo(config, (EntityPlayerMP) evt.player);
        }

    }

}
