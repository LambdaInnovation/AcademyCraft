package cn.li.forge1201.gametest;

import clojure.lang.Keyword;
import cn.li.mcbase.clj.ClojureInterop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

import static cn.li.forge1201.AcademyCraft1201.MODID;

/**
 * Neutral Forge smoke tests for platform bootstrap and descriptor registration.
 *
 * {@link PrefixGameTestTemplate}({@code false}) makes {@code template = "empty"}
 * resolve to {@code minecraft:empty}. Without it, Forge prepends the lowercase
 * class name to the template id.
 */
@GameTestHolder(MODID)
@PrefixGameTestTemplate(false)
public final class ForgeSmokeGameTests {
    private ForgeSmokeGameTests() {
    }

    private static Keyword kw(String name) {
        return Keyword.intern(null, name);
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", batch = "platform_smoke")
    public static void neutralFeatureRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.FEATURE.containsKey(ResourceLocation.fromNamespaceAndPath(MODID, "configurable_pool")),
            "Expected neutral configurable_pool feature to be registered");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", batch = "content_smoke")
    public static void contentSmokeManifestsRegistered(GameTestHelper helper) {
        String registryNs = "cn.li.mcmod.content.registry";
        ClojureInterop.requireNamespace(registryNs);

        Object manifestsObj = ClojureInterop.invoke(registryNs, "list-smoke-manifests");
        helper.assertTrue(manifestsObj instanceof Iterable,
            "Expected content smoke manifest registry to be iterable");

        boolean foundManifestWithChecks = false;
        for (Object manifestObj : (Iterable<?>) manifestsObj) {
            if (manifestObj instanceof Map<?, ?> manifestMap && manifestMap.containsKey(kw("checks"))) {
                foundManifestWithChecks = true;
                break;
            }
        }

        helper.assertTrue(foundManifestWithChecks,
            "Expected at least one content-owned smoke manifest with checks");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", batch = "content_smoke")
    public static void electronBombDefaultsAligned(GameTestHelper helper) {
        String registryNs = "cn.li.mcmod.content.registry";
        ClojureInterop.requireNamespace(registryNs);

        Object manifestsObj = ClojureInterop.invoke(registryNs, "list-smoke-manifests");
        helper.assertTrue(manifestsObj instanceof Iterable,
            "Expected content smoke manifest registry to be iterable");

        Map<?, ?> electronBombDefaults = null;
        for (Object manifestObj : (Iterable<?>) manifestsObj) {
            if (manifestObj instanceof Map<?, ?> manifestMap
                && "ac".equals(manifestMap.get(kw("content-id")))) {
                Object fixturesObj = manifestMap.get(kw("fixtures"));
                if (fixturesObj instanceof Map<?, ?> fixturesMap) {
                    Object defaultsObj = fixturesMap.get(kw("electron-bomb-defaults"));
                    if (defaultsObj instanceof Map<?, ?> defaultsMap) {
                        electronBombDefaults = defaultsMap;
                        break;
                    }
                }
            }
        }

        helper.assertTrue(electronBombDefaults != null,
            "Expected AC smoke manifest to expose :electron-bomb-defaults fixture");

        Object delayObj = electronBombDefaults.get(kw("settle-delay-ticks"));
        helper.assertTrue(delayObj instanceof Number && ((Number) delayObj).intValue() == 15,
            "Expected electron-bomb delayed settlement default to be 15 ticks (life 20, settle offset 5)");

        Object damageEndpointsObj = electronBombDefaults.get(kw("combat-damage"));
        helper.assertTrue(damageEndpointsObj instanceof List<?>,
            "Expected electron-bomb combat damage endpoints fixture to be list-like");

        List<?> damageEndpoints = (List<?>) damageEndpointsObj;
        helper.assertTrue(damageEndpoints.size() == 2,
            "Expected electron-bomb combat.damage endpoints to have length 2");
        helper.assertTrue(((Number) damageEndpoints.get(0)).doubleValue() == 6.0,
            "Expected electron-bomb min damage endpoint to be 6.0");
        helper.assertTrue(((Number) damageEndpoints.get(1)).doubleValue() == 12.0,
            "Expected electron-bomb max damage endpoint to be 12.0");

        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", batch = "content_smoke")
    public static void scatterBombDefaultsAligned(GameTestHelper helper) {
        String registryNs = "cn.li.mcmod.content.registry";
        ClojureInterop.requireNamespace(registryNs);

        Object manifestsObj = ClojureInterop.invoke(registryNs, "list-smoke-manifests");
        helper.assertTrue(manifestsObj instanceof Iterable,
            "Expected content smoke manifest registry to be iterable");

        Map<?, ?> scatterDefaults = null;
        for (Object manifestObj : (Iterable<?>) manifestsObj) {
            if (manifestObj instanceof Map<?, ?> manifestMap
                && "ac".equals(manifestMap.get(kw("content-id")))) {
                Object fixturesObj = manifestMap.get(kw("fixtures"));
                if (fixturesObj instanceof Map<?, ?> fixturesMap) {
                    Object defaultsObj = fixturesMap.get(kw("scatter-bomb-defaults"));
                    if (defaultsObj instanceof Map<?, ?> defaultsMap) {
                        scatterDefaults = defaultsMap;
                        break;
                    }
                }
            }
        }

        helper.assertTrue(scatterDefaults != null,
            "Expected AC smoke manifest to expose :scatter-bomb-defaults fixture");

        Object maxHoldObj = scatterDefaults.get(kw("max-hold-ticks"));
        helper.assertTrue(maxHoldObj instanceof Number && ((Number) maxHoldObj).intValue() == 80,
            "Expected scatter-bomb hold window to cap at 80 ticks");

        Object antiAfkTickObj = scatterDefaults.get(kw("anti-afk-tick"));
        helper.assertTrue(antiAfkTickObj instanceof Number && ((Number) antiAfkTickObj).intValue() == 200,
            "Expected scatter-bomb anti-afk trigger at 200 ticks");

        Object antiAfkDamageObj = scatterDefaults.get(kw("anti-afk-damage"));
        helper.assertTrue(antiAfkDamageObj instanceof Number
                && ((Number) antiAfkDamageObj).doubleValue() == 6.0,
            "Expected scatter-bomb anti-afk self damage to be 6.0");

        Object settleDelayObj = scatterDefaults.get(kw("settle-delay-ticks"));
        helper.assertTrue(settleDelayObj instanceof Number && ((Number) settleDelayObj).intValue() == 15,
            "Expected scatter-bomb delayed settlement default to be 15 ticks (life 20, settle offset 5)");

        helper.succeed();
    }
}
