package cn.li.forge1201.event;

import cn.li.mcmod.ModId;
import cn.li.forge1201.AcademyCraft1201;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = AcademyCraft1201.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEventBusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModId.ID);

    private ForgeEventBusManager() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ForgeCommandRegistrar.registerAll(event, LOGGER);
    }
}
