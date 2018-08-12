package cn.academy;

import cn.academy.network.NetworkManager;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.ResourceUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;

public final class ACConfig {

    private ACConfig() {}

    private static Config config;

    private static String lastError = null;

    private static void __init() {
        Logger log = AcademyCraft.log;

        ResourceLocation defaultRes = new ResourceLocation("academy:config/default.conf");

        Reader reader = new InputStreamReader(ResourceUtils.getResourceStream(defaultRes));

        config = ConfigFactory.parseReader(reader);

        File customFile = new File("config/academy-craft-data.conf");
        if (!customFile.isFile()) {
            try {
                Files.copy(ResourceUtils.getResourceStream(defaultRes), customFile.toPath());
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


    @StateEventCallback
    private static void __onInit(FMLInitializationEvent event) {
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
                evt.player.sendMessage(new TextComponentTranslation("ac.data_config_parse_fail"));
                evt.player.sendMessage(new TextComponentTranslation(err));
            }
            if(!evt.player.world.isRemote)
                NetworkManager.sendTo(config, (EntityPlayerMP) evt.player);
        }
    }

}