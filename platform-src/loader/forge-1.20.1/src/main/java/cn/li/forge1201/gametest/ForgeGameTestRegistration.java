package cn.li.forge1201.gametest;

import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

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