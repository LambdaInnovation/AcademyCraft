package cn.academy.core.config;

import cn.lambdalib.util.generic.RegistryUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.io.Reader;

public final class ACConfig {

    private ACConfig() {}

    private static Config config;

    private static void __init() {
        Reader reader = new InputStreamReader(RegistryUtils.getResourceStream(
                new ResourceLocation("academy:config/default.conf")));
        config = ConfigFactory.parseReader(reader);
    }

    public static Config instance() {
        synchronized (ACConfig.class) {
            if (config == null) {
                __init();
            }

            return config;
        }
    }

}
