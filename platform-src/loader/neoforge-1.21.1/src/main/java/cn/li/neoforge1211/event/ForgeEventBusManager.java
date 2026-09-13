package cn.li.neoforge1211.event;

import cn.li.neoforgebase.event.ForgeCommandRegistrar;

import cn.li.mcmod.ModId;
import cn.li.neoforge1211.AcademyCraft1211;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = AcademyCraft1211.MODID)
public final class ForgeEventBusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModId.ID);

    private ForgeEventBusManager() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ForgeCommandRegistrar.registerAll(event, LOGGER, "cn.li.neoforge1211.commands");
    }
}
