package cn.li.neoforge262.gametest;

import cn.li.mcver.ResourceLocations;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Registers smoke tests through the data-driven 26.2 GameTest registry.
 *
 * <p>The 26.2 TEST_INSTANCE registry is synchronized to clients through a
 * dispatch codec ({@code GameTestInstance.DIRECT_CODEC}): the "function"
 * type encodes with {@code FunctionGameTestInstance.CODEC}, whose field
 * getters cast the value to FunctionGameTestInstance. A custom
 * GameTestInstance subclass therefore fails that cast the moment the
 * integrated server packs registries for a joining client, so the smoke
 * tests must extend FunctionGameTestInstance. The function key is never
 * resolved: run() is overridden to call the body directly.
 */
public final class ForgeGameTestRegistration {
    private static final String MODID = "academy";

    private ForgeGameTestRegistration() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ForgeGameTestRegistration::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(ResourceLocations.of(MODID, "smoke"));

        register(event, environment, "neutral_feature_registered",
                ForgeSmokeGameTests::neutralFeatureRegistered);
        register(event, environment, "content_smoke_manifests_registered",
                ForgeSmokeGameTests::contentSmokeManifestsRegistered);
        register(event, environment, "electron_bomb_defaults_aligned",
                ForgeSmokeGameTests::electronBombDefaultsAligned);
        register(event, environment, "scatter_bomb_defaults_aligned",
                ForgeSmokeGameTests::scatterBombDefaultsAligned);
    }

    private static void register(RegisterGameTestsEvent event,
                                 Holder<TestEnvironmentDefinition<?>> environment,
                                 String name,
                                 Consumer<GameTestHelper> body) {
        Identifier id = ResourceLocations.of(MODID, name);
        ResourceKey<Consumer<GameTestHelper>> functionKey =
                ResourceKey.create(Registries.TEST_FUNCTION, id);
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(environment, ResourceLocations.of("minecraft", "empty"), 100, 0, true);
        event.registerTest(id, new SmokeTestInstance(functionKey, data, name, body));
    }

    private static final class SmokeTestInstance extends FunctionGameTestInstance {
        private final String name;
        private final Consumer<GameTestHelper> body;

        private SmokeTestInstance(ResourceKey<Consumer<GameTestHelper>> function,
                                  TestData<Holder<TestEnvironmentDefinition<?>>> data,
                                  String name,
                                  Consumer<GameTestHelper> body) {
            super(function, data);
            this.name = name;
            this.body = body;
        }

        @Override
        public void run(GameTestHelper helper) {
            body.accept(helper);
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Academy smoke: " + name);
        }
    }
}
