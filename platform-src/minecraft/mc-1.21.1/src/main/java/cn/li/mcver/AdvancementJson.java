package cn.li.mcver;

/**
 * Datagen advancement JSON shape differences across MC versions.
 */
public final class AdvancementJson {
    private AdvancementJson() {
    }

    /** Pack path segment under {@code data/<modid>/}. */
    public static String dataFolder() {
        return "advancements";
    }

    /** Display icon object key in advancement JSON ({@code item} vs {@code id}). */
    public static String iconKey() {
        return "item";
    }
}
