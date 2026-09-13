package cn.li.mcver;

/**
 * Datagen advancement JSON shape differences across MC versions.
 */
public final class AdvancementJson {
    private AdvancementJson() {
    }

    /** Pack path segment under {@code data/<modid>/} (singular on 26.2). */
    public static String dataFolder() {
        return "advancement";
    }

    /** Display icon object key in advancement JSON ({@code id} on 26.2). */
    public static String iconKey() {
        return "id";
    }
}
