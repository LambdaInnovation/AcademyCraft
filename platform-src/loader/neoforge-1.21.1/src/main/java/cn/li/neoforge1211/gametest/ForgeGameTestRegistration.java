package cn.li.neoforge1211.gametest;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.bus.api.IEventBus;

/** Registers Forge GameTest classes even when dev classpath annotation scan misses them. */
public final class ForgeGameTestRegistration {
    private ForgeGameTestRegistration() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ForgeGameTestRegistration::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(ForgeSmokeGameTests.class);
    }
}