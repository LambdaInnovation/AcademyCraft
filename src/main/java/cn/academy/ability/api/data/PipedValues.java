package cn.academy.ability.api.data;

/**
 * Global key reference of dynamically piped values' location.
 */
public class PipedValues {

    public static final String
        MAXCP = abilityPath("max_cp"),
        MAXOVERLOAD = abilityPath("max_overload");

    public static final String
        ANY_DAMAGE      = abilityPath("any_damage"),
        ANY_CONSUMPTION = abilityPath("any_cp"),
        ANY_OVERLOAD    = abilityPath("any_overload");

    private static String abilityPath(String name) {
        return "ac.dynamic.ability." + name;
    }

}
