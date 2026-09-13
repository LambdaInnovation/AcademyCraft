package cn.li.neoforge262.event;

import cn.li.neoforgebase.event.ForgeCommandRegistrar;

import cn.li.mcmod.ModId;
import cn.li.neoforge262.AcademyCraft262;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 26.2: EventBusSubscriber.Bus was removed. Register game-bus listeners explicitly.
 */
public final class ForgeEventBusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModId.ID);
    private static volatile boolean registered;

    private ForgeEventBusManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(ForgeEventBusManager::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        ForgeCommandRegistrar.registerAll(event, LOGGER, "cn.li.neoforge262.commands");
    }

    /** Kept for callers that still reference the old MODID constant path. */
    @SuppressWarnings("unused")
    private static final String MODID = AcademyCraft262.MODID;
}
