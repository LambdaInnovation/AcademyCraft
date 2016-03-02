package cn.academy.core.config;

import cn.academy.core.AcademyCraft;
import com.google.common.base.Throwables;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

public final class ACConfig {

    private ACConfig() {}

    private static Config config;

    private static void __init() {
        try {
            String basePath = "/assets/academy/config/";

            List<String> files = IOUtils.readLines(ACConfig.class.getResourceAsStream(basePath));
            AcademyCraft.log.info("AC: Loading config files " + files);

            for (String filename : files) {
                Config conf = ConfigFactory.parseResourcesAnySyntax(ACConfig.class, basePath + filename);
                if (config != null) {
                    config = config.withFallback(conf);
                } else {
                    config = conf;
                }
            }
        } catch (IOException ex) {
            Throwables.propagate(ex);
        }
    }

    public static Config instance() {
        synchronized (ACConfig.class) {
            if (config == null) {
                __init();
            }
        }
        return config;
    }

}
