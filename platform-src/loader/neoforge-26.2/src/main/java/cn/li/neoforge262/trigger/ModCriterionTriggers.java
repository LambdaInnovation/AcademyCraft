package cn.li.neoforge262.trigger;

import cn.li.mc262.trigger.ModCustomTrigger;
import cn.li.mc262.trigger.ModTriggers;
import cn.li.neoforge262.AcademyCraft262;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom criterion triggers via DeferredRegister.
 */
public final class ModCriterionTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, AcademyCraft262.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, ModCustomTrigger> CUSTOM =
            TRIGGERS.register("custom", () -> ModTriggers.CUSTOM);

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }

    private ModCriterionTriggers() {
    }
}
