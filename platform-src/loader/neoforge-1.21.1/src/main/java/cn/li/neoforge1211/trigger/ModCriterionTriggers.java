package cn.li.neoforge1211.trigger;

import cn.li.mc1211.trigger.ModCustomTrigger;
import cn.li.mc1211.trigger.ModTriggers;
import cn.li.neoforge1211.AcademyCraft1211;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom criterion triggers via DeferredRegister.
 *
 * <p>Never call vanilla {@code CriteriaTriggers.register} from mod construct —
 * the trigger_type registry is already frozen by then.
 */
public final class ModCriterionTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
        DeferredRegister.create(Registries.TRIGGER_TYPE, AcademyCraft1211.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, ModCustomTrigger> CUSTOM =
        TRIGGERS.register("custom", () -> ModTriggers.CUSTOM);

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }

    private ModCriterionTriggers() {}
}
