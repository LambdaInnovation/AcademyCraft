package cn.academy.core;

import net.minecraft.util.StatCollector;

/**
 * This class wraps around Minecraft locallization with namespace helper support.
 * All localization codes should not reference MC code and use this.
 */
public class LocalHelper {

    public static final LocalHelper root = at("");

    public static LocalHelper at(String path) {
        return new LocalHelper(path);
    }

    private final String path;

    public String get(String id) {
        return StatCollector.translateToLocal(path + id);
    }

    public String getFormatted(String id, Object ...args) {
        return StatCollector.translateToLocalFormatted(path + id, args);
    }

    public LocalHelper subPath(String id) {
        return new LocalHelper(path + id);
    }

    private LocalHelper(String _path) {
        if (_path.isEmpty() || _path.endsWith(".")) {
            path = _path;
        } else {
            path = _path + ".";
        }
    }

}
