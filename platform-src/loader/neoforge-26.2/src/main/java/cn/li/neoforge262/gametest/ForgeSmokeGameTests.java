package cn.li.neoforge262.gametest;

import clojure.lang.Keyword;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mcver.ResourceLocations;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Neutral smoke-test bodies for the data-driven 26.2 GameTest registry.
 */
public final class ForgeSmokeGameTests {
    private static final String MODID = "academy";
    private static final String REGISTRY_NS = "cn.li.mcmod.content.registry";

    private ForgeSmokeGameTests() {
    }

    private static Keyword kw(String name) {
        return Keyword.intern(null, name);
    }

    public static void neutralFeatureRegistered(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.FEATURE.containsKey(ResourceLocations.of(MODID, "configurable_pool")),
                "Expected neutral configurable_pool feature to be registered");
        helper.succeed();
    }

    public static void contentSmokeManifestsRegistered(GameTestHelper helper) {
        ClojureInterop.requireNamespace(REGISTRY_NS);
        Object manifestsObj = ClojureInterop.invoke(REGISTRY_NS, "list-smoke-manifests");
        helper.assertTrue(manifestsObj instanceof Iterable,
                "Expected content smoke manifest registry to be iterable");

        boolean found = false;
        for (Object manifestObj : (Iterable<?>) manifestsObj) {
            if (manifestObj instanceof Map<?, ?> manifest && manifest.containsKey(kw("checks"))) {
                found = true;
                break;
            }
        }
        helper.assertTrue(found, "Expected at least one content-owned smoke manifest with checks");
        helper.succeed();
    }

    public static void electronBombDefaultsAligned(GameTestHelper helper) {
        Map<?, ?> defaults = fixture("electron-bomb-defaults");
        helper.assertTrue(defaults != null,
                "Expected AC smoke manifest to expose :electron-bomb-defaults fixture");
        Object delay = defaults.get(kw("settle-delay-ticks"));
        helper.assertTrue(delay instanceof Number && ((Number) delay).intValue() == 15,
                "Expected electron-bomb delayed settlement default to be 15 ticks");
        Object damageObj = defaults.get(kw("combat-damage"));
        helper.assertTrue(damageObj instanceof List<?>,
                "Expected electron-bomb combat damage endpoints fixture to be list-like");
        List<?> damage = (List<?>) damageObj;
        helper.assertTrue(damage.size() == 2
                        && ((Number) damage.get(0)).doubleValue() == 6.0
                        && ((Number) damage.get(1)).doubleValue() == 12.0,
                "Expected electron-bomb combat damage endpoints [6.0, 12.0]");
        helper.succeed();
    }

    public static void scatterBombDefaultsAligned(GameTestHelper helper) {
        Map<?, ?> defaults = fixture("scatter-bomb-defaults");
        helper.assertTrue(defaults != null,
                "Expected AC smoke manifest to expose :scatter-bomb-defaults fixture");
        helper.assertTrue(numberEquals(defaults, "max-hold-ticks", 80.0),
                "Expected scatter-bomb hold window to cap at 80 ticks");
        helper.assertTrue(numberEquals(defaults, "anti-afk-tick", 200.0),
                "Expected scatter-bomb anti-afk trigger at 200 ticks");
        helper.assertTrue(numberEquals(defaults, "anti-afk-damage", 6.0),
                "Expected scatter-bomb anti-afk self damage to be 6.0");
        helper.assertTrue(numberEquals(defaults, "settle-delay-ticks", 15.0),
                "Expected scatter-bomb delayed settlement default to be 15 ticks");
        helper.succeed();
    }

    private static boolean numberEquals(Map<?, ?> values, String key, double expected) {
        Object value = values.get(kw(key));
        return value instanceof Number && ((Number) value).doubleValue() == expected;
    }

    private static Map<?, ?> fixture(String fixtureName) {
        ClojureInterop.requireNamespace(REGISTRY_NS);
        Object manifestsObj = ClojureInterop.invoke(REGISTRY_NS, "list-smoke-manifests");
        if (!(manifestsObj instanceof Iterable<?> manifests)) {
            return null;
        }
        for (Object manifestObj : manifests) {
            if (manifestObj instanceof Map<?, ?> manifest
                    && "ac".equals(manifest.get(kw("content-id")))
                    && manifest.get(kw("fixtures")) instanceof Map<?, ?> fixtures
                    && fixtures.get(kw(fixtureName)) instanceof Map<?, ?> defaults) {
                return defaults;
            }
        }
        return null;
    }
}
