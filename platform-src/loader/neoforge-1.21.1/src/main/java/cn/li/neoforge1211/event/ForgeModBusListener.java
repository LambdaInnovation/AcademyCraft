package cn.li.neoforge1211.event;

import cn.li.mcbase.datagen.DataGeneratorInterop;
import cn.li.neoforge1211.AcademyCraft1211;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AcademyCraft1211.MODID)
public final class ForgeModBusListener {
    private ForgeModBusListener() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGeneratorInterop.invoke(
            "[academy] Error invoking Clojure DataGenerator handler: ",
            "cn.li.neoforge1211.datagen.setup",
            "static-gather-data",
            event);
    }
}
